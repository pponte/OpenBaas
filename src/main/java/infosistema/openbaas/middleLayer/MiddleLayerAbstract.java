package infosistema.openbaas.middleLayer;

import java.util.ArrayList;
import java.util.List;

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
import infosistema.openbaas.dataaccess.models.AppModel;
import infosistema.openbaas.dataaccess.models.DocumentModel;
import infosistema.openbaas.dataaccess.models.MediaModel;
import infosistema.openbaas.dataaccess.models.SessionModel;
import infosistema.openbaas.dataaccess.models.UserModel;
import infosistema.openbaas.utils.Utils;

public abstract class MiddleLayerAbstract {

	// *** MEMBERS *** //

	protected AppModel appModel;
	protected UserModel userModel;
	protected DocumentModel docModel;
	protected SessionModel sessionsModel;
	protected MediaModel mediaModel;

	
	// *** INIT *** //
	
	protected MiddleLayerAbstract() {
		docModel = new DocumentModel();
		appModel = new AppModel();;
		userModel = new UserModel();
		sessionsModel = new SessionModel();
		mediaModel = new MediaModel();
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
		List<String> listRes = getAllSearchResults(qp.getAppId(), qp.getUserId(), qp.getUrl(), qp.getLatitude(), qp.getLongitude(), qp.getRadius(),
				qp.getQuery(), qp.getOrderType(), qp.getOrderBy(), qp.getType());
		return paginate(listRes, qp.getPageNumber(), qp.getPageSize());
	}

	protected abstract List<String> getAllSearchResults(String appId, String userId, String url, Double latitude, Double longitude, Double radius, JSONObject query, String orderType, String orderBy, ModelEnum type) throws Exception;

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
	
	private ListResult paginate(List<String> lst, Integer pageNumber, Integer pageSize) {
		List<String> listRes = new ArrayList<String>();
		Integer iniIndex = (pageNumber-1)*pageSize;
		Integer finIndex = (((pageNumber-1)*pageSize)+pageSize);
		if (finIndex > lst.size()) finIndex = lst.size();
		try { listRes = lst.subList(iniIndex, finIndex); } catch (Exception e) {}
		Integer totalElems = (int) Utils.roundUp(lst.size(),pageSize);
		ListResult listResultRes = new ListResult(listRes, pageNumber, pageSize, lst.size(),totalElems);
		return listResultRes;
	}
	
}
