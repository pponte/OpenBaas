package infosistema.openbaas.middleLayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.PathSegment;

import org.codehaus.jettison.json.JSONObject;

import infosistema.openbaas.data.ListResult;
import infosistema.openbaas.data.QueryParameters;
import infosistema.openbaas.data.enums.FileMode;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.dataaccess.files.AwsModel;
import infosistema.openbaas.dataaccess.files.FileInterface;
import infosistema.openbaas.dataaccess.files.FileSystemModel;
import infosistema.openbaas.dataaccess.files.FtpModel;
import infosistema.openbaas.dataaccess.geolocation.Geolocation;
import infosistema.openbaas.dataaccess.models.AppModel;
import infosistema.openbaas.dataaccess.models.DocumentModel;
import infosistema.openbaas.dataaccess.models.MediaModel;
import infosistema.openbaas.dataaccess.models.SessionModel;
import infosistema.openbaas.dataaccess.models.UserModel;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

public abstract class MiddleLayerAbstract {

	// *** MEMBERS *** //

	protected AppModel appModel;
	protected UserModel userModel;
	protected DocumentModel docModel;
	protected SessionModel sessionsModel;
	protected MediaModel mediaModel;
	protected Geolocation geo;

	
	// *** INIT *** //
	
	protected MiddleLayerAbstract() {
		docModel = new DocumentModel();
		appModel = new AppModel();;
		userModel = new UserModel();
		sessionsModel = new SessionModel();
		mediaModel = new MediaModel();
		geo = Geolocation.getInstance();
	}

	// *** FILESYSTEM *** //
	
	protected FileInterface getAppFileInterface(String appId) {
		FileMode appFileMode = appModel.getApplicationFileMode(appId);
		if (appFileMode == FileMode.aws) return AwsModel.getInstance();
		else if (appFileMode == FileMode.ftp) return FtpModel.getInstance();
		else return FileSystemModel.getInstance();
	}
	

	// *** PROTECTED *** //
	
	protected List<String> convertPath(List<PathSegment> path) {
		List<String> retObj = new ArrayList<String>();
		if (path != null) {
			for(PathSegment pathSegment: path) {
				if (pathSegment.getPath() != null && !"".equals(pathSegment.getPath()))
					retObj.add(pathSegment.getPath());
			}
		}
		return retObj;
	}
	
	
	// *** GET LIST *** //

	public ListResult find(QueryParameters qp) throws Exception {
		List<String> listRes = new ArrayList<String>();
		List<String> list1 = getAllSearchResults(qp.getAppId(), qp.getUserId(), qp.getUrl(), qp.getQuery(), qp.getOrderType(), qp.getType());
		List<String> list2 = new ArrayList<String>();
		if (qp.getLatitude() != null && qp.getLongitude() != null && qp.getRadius()!= null){
			list2 = geo.getObjectsInDistance(qp.getLatitude(), qp.getLongitude(), qp.getRadius(), qp.getAppId(), qp.getType());
			listRes = and(list1, list2);
		}else {
			listRes = list1;
		}
		
		return paginate(qp.getAppId(), listRes, qp.getOrderBy(), qp.getOrderType(), qp.getPageNumber(),
				qp.getPageSize(), qp.getType());
	}

	protected abstract List<String> getAllSearchResults(String appId, String userId, String url, JSONObject query, String orderType, ModelEnum type) throws Exception;

	protected List<String> and(List<String> list1, List<String> list2) {
		List<String> lOrig = list1.size() > list2.size() ? list2 : list1; 
		List<String> lComp = list1.size() > list2.size() ? list1 : list2; 
		List<String> lDest = new ArrayList<String>(); 
		for (String id: lOrig) {
			if (lComp.contains(id))
				lDest.add(id);
		}
		return lDest;
	}
	
	protected List<String> getAll(String appId, ModelEnum type) throws Exception {
		return new ArrayList<String>();
	}
	
	private ListResult paginate(String appId, List<String> lst, String orderBy, String orderType, 
			Integer pageNumber, Integer pageSize, ModelEnum type) {
		
		ArrayList<String> listIdsSorted = new ArrayList<String>();
		List<String> listRes = new ArrayList<String>();
		Map<String, String> hash = new HashMap<String, String>();
		Iterator<String> it = lst.iterator();
		while(it.hasNext()){
			Object value = null;
			String key = it.next();
			if(type.compareTo(ModelEnum.audio)==0 ||type.compareTo(ModelEnum.video)==0 ||
			   type.compareTo(ModelEnum.storage)==0 ||type.compareTo(ModelEnum.image)==0){
				if(orderBy.equals("_id")&&type.compareTo(ModelEnum.audio)==0)
					orderBy="audioId";
				if(orderBy.equals("_id")&&type.compareTo(ModelEnum.video)==0)
					orderBy="videoId";
				if(orderBy.equals("_id")&&type.compareTo(ModelEnum.storage)==0)
					orderBy="storageId";
				if(orderBy.equals("_id")&&type.compareTo(ModelEnum.image)==0)
					orderBy="imageId";
				JSONObject temp = mediaModel.getMedia(appId, type, key, false);
				try {
					value = temp.getString(orderBy);
				} catch (Exception e) {}
			}
			if(type.compareTo(ModelEnum.users)==0){
				if(orderBy.equals("_id"))
					orderBy = Const.USER_ID;
				JSONObject temp = userModel.getUser(appId, key, false);
				try {
					value = temp.getString(orderBy);
				} catch (Exception e) {
					Log.error("", this, "paginate", "Error getting orderBy field (" + orderBy + ")", e);
				}
			}
			if(type.compareTo(ModelEnum.data)==0){
				//TODO Nota JM: Nao esta implementado nem me parece possivel.
			}
			if (value == null) value = "_id";
			hash.put(key, value.toString());
		}

		
		Map<String, String> hashSorted = sortByValues(hash);
		
		Iterator<Entry<String,String>> entries = hashSorted.entrySet().iterator();
		while (entries.hasNext()) {
		  Entry<String,String> thisEntry = entries.next();
		  String key = thisEntry.getKey();
		  listIdsSorted.add(key);
		}
		if(orderType.equals("desc")){
			Collections.reverse(listIdsSorted);
		}

		Integer iniIndex = (pageNumber-1)*pageSize;
		Integer finIndex = (((pageNumber-1)*pageSize)+pageSize);
		
		if(finIndex>listIdsSorted.size())
			try{listRes  = listIdsSorted.subList(iniIndex, listIdsSorted.size());}catch(Exception e){}
		else{
			try{listRes = listIdsSorted.subList(iniIndex, finIndex);}catch(Exception e){}
		}
			
		Integer totalElems = (int) Utils.roundUp(listIdsSorted.size(),pageSize);
		ListResult listResultRes = new ListResult(listRes, pageNumber, pageSize, lst.size(),totalElems);
		return listResultRes;
	}
	
	private static Map<String, String> sortByValues(Map<String, String> map){
        List<Map.Entry<String, String>> entries = new LinkedList<Map.Entry<String, String>>(map.entrySet());
        
        Collections.sort(entries, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Entry<String, String> o1, Entry<String, String> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
     
        //LinkedHashMap will keep the keys in the order they are inserted
        //which is currently sorted on natural ordering
        Map<String, String> sortedMap = new LinkedHashMap<String, String>();
     
        for(Map.Entry<String, String> entry: entries){
            sortedMap.put(entry.getKey(), entry.getValue());
        }
     
        return sortedMap;
    }

}
