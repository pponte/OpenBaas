package infosistema.openbaas.middleLayer;


import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.dataaccess.models.ModelAbstract;
import infosistema.openbaas.utils.Log;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.PathSegment;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

public class DocumentMiddleLayer extends MiddleLayerAbstract {

	// *** MEMBERS *** //

	// *** INSTANCE *** //
	
	private static DocumentMiddleLayer instance = null;

	public static DocumentMiddleLayer getInstance() {
		if (instance == null) instance = new DocumentMiddleLayer();
		return instance;
	}
	
	private DocumentMiddleLayer() {
		super();
	}

	
	// *** PRIVATE *** //

	public String convertPathToString(List<PathSegment> path) {
		StringBuilder sb = new StringBuilder();
		if (path != null && !path.isEmpty()) {
			for(int i = 0; i < path.size(); i++)
				sb.append(path.get(i).getPath()).append('.');
			sb.deleteCharAt(sb.length()-1);
		}
		return sb.toString();
	}

	public String getDocumentPath(String userId, List<PathSegment> path) {
		return docModel.getDocumentPath(convertPath(path));
	}


	// *** CREATE *** //
	
	public Result insertDocumentInPath(String appId, String userId, List<PathSegment> path, JSONObject document, String location, Map<String, String> extraMetadata) {
		try {
			Metadata metadata = null;
			Object data = null;
			List<String> lPath = convertPath(path);
			String id = docModel.getDocumentId(userId, lPath);
			data = docModel.insertDocumentInPath(appId, userId, lPath, document, extraMetadata);
			metadata = Metadata.getMetadata(((JSONObject) data).getJSONObject(ModelAbstract._METADATA));
			((JSONObject) data).remove(ModelAbstract._METADATA);
			data = (DBObject)JSON.parse(data.toString());
			if (location != null) {
				String[] splitted = location.split(":");
				geo.insertObjectInGrid(Double.parseDouble(splitted[0]),	Double.parseDouble(splitted[1]), ModelEnum.data, appId, id);
			}
			return new Result(data, metadata);
		} catch (JSONException e) {
			Log.error("", this, "insertDocumentInPath", "Error parsing the JSON.", e); 
		} catch (Exception e) {
			Log.error("", this, "insertDocumentInPath", "An error ocorred.", e); 
		}
		return null;
	}


	// *** UPDATE *** //
	
	public Result updateDocumentInPath(String appId, String userId, List<PathSegment> path, JSONObject document, String location, Map<String, String> extraMetadata) {
		try {
			Metadata metadata = null;
			Object data = null;
			List<String> lPath = convertPath(path);
			String id = docModel.getDocumentId(userId, lPath);
			data = docModel.updateDocumentInPath(appId, userId, lPath, document, extraMetadata);
			metadata = Metadata.getMetadata(((JSONObject) data).getJSONObject(ModelAbstract._METADATA));
			((JSONObject) data).remove(ModelAbstract._METADATA);
			data = (DBObject)JSON.parse(data.toString());
			if (location != null){
				String[] splitted = location.split(":");
				geo.insertObjectInGrid(Double.parseDouble(splitted[0]),	Double.parseDouble(splitted[1]), ModelEnum.data, appId, id);
			}
			return new Result(data, metadata);
		} catch (JSONException e) {
			Log.error("", this, "updateDocumentInPath", "Error parsing the JSON.", e); 
		} catch (Exception e) {
			Log.error("", this, "updateDocumentInPath", "An error ocorred.", e); 
		}
		return null;
	}

	
	// *** DELETE *** //

	public boolean deleteDocumentInPath(String appId, String userId, List<PathSegment> path) {
		Boolean res = false;
		try {
			res = docModel.deleteDocumentInPath(appId, userId, convertPath(path));
		} catch (Exception e) {
			Log.error("", this, "deleteDocumentInPath", "An error ocorred.", e); 
			return false;
		}
		return res;
	}
	
	
	// *** GET LIST *** //

	@Override
	protected List<String> getAllSearchResults(String appId, String userId, String url, JSONObject query, String orderType, ModelEnum type) throws Exception {
		return docModel.getDocuments(appId, userId, url, query, orderType);
	}

	
	// *** GET *** //
	
	public Result getDocumentInPath(String appId, String userId, List<PathSegment> path, boolean getMetadata) {
		Metadata metadata = null;
		Object data = null;
		try {
			data = docModel.getDocumentInPath(appId, userId, convertPath(path), getMetadata);
			if (data instanceof JSONObject) {
				if (getMetadata) {
					metadata = Metadata.getMetadata(((JSONObject) data).getJSONObject(ModelAbstract._METADATA));
					((JSONObject) data).remove(ModelAbstract._METADATA);
					data = (DBObject)JSON.parse(data.toString());
				}
			}
		} catch (Exception e) {
			Log.error("", this, "getDocumentInPath", "An error ocorred.", e); 
			return null;
		}
		return new Result(data, metadata);
	}
	
	// *** EXISTS *** //

	public boolean existsDocumentInPath(String appId, String userId, List<PathSegment> path) {
		try {
			return docModel.existsDocument(appId, userId, convertPath(path));
		} catch (Exception e) {
			Log.error("", this, "existsDocumentInPath", "An error ocorred.", e); 
			return false;
		}
	}

	
	// *** OTHERS *** //
	
}
