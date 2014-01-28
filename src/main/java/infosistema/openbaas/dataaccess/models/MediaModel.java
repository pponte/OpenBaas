package infosistema.openbaas.dataaccess.models;

import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.enums.OperatorEnum;
import infosistema.openbaas.utils.Log;

import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

public class MediaModel extends ModelAbstract {

	// request types
	public static final String APP_MEDIA_COLL_FORMAT = "app%smedia";
	protected static final String _TYPE = "_type";
	private static final String TYPE_QUERY_FORMAT = "{" + _TYPE + ": \"%s\"";

	private static BasicDBObject dataProjection = null; 	
	private static BasicDBObject dataProjectionMetadata = null; 	

	public MediaModel() {
	}

	// *** *** MEDIA *** *** //
	
	// *** PRIVATE *** //
	
	@Override
	protected DBCollection getCollection(String appId) {
		return super.getCollection(String.format(APP_MEDIA_COLL_FORMAT, appId));
	}
	
	@Override
	protected BasicDBObject getDataProjection(boolean getMetadata) {
		if (getMetadata) {
			if (dataProjectionMetadata == null) {
				dataProjectionMetadata = super.getDataProjection(new BasicDBObject(), true);
				dataProjectionMetadata.append(_TYPE, ZERO);
			}
			return dataProjectionMetadata;
		} else {
			if (dataProjection == null) {
				dataProjection = super.getDataProjection(new BasicDBObject(), false);
				dataProjection.append(_TYPE, ZERO);
			}
			return dataProjection;
		}
	}

	private String getTypeQuery(ModelEnum type) {
		if (type == null) return "";
		return String.format(TYPE_QUERY_FORMAT, type.toString());
	}

	// *** CREATE *** //
	
	public JSONObject createMedia(String appId, String userId, ModelEnum type, String objId, Map<String, String> mediaFields, Map<String, String> extraMetadata) {
		try{
			if (type == null) {
				Log.error("", this, "createMedia", "Media as no type.");
				return null;
			}
			JSONObject data = getJSonObject(mediaFields);
			data.put(_ID, objId);
			data.put(_TYPE, type.toString());
			return super.insert(appId, data, getMetadaJSONObject(getMetadataCreate(userId, extraMetadata)));
		} catch (Exception e) {
			Log.error("", this, "createMedia", "An error ocorred.", e);
		}
		return null;
	}
	
	
	// *** UPDATE *** //

	// *** GET LIST *** //
	
	public List<String> getMedia(String appId, ModelEnum type, JSONObject query, String orderType) throws Exception {
		JSONObject finalQuery = new JSONObject();
		if (type != null) {
			finalQuery.append(OperatorEnum.oper.toString(), OperatorEnum.and.toString());
			finalQuery.append(OperatorEnum.op1.toString(), getTypeQuery(type));
			finalQuery.append(OperatorEnum.op2.toString(), query.toString());
		} else {
			finalQuery = query;
		}
		return super.getDocuments(appId, null, null, finalQuery, orderType);
	}

	// *** GET *** //

	public JSONObject getMedia(String appId, ModelEnum type, String objId, boolean getMetadata) {
		//CACHE
		try {
			return super.getDocument(appId, objId, getMetadata);
		} catch (JSONException e) {
			Log.error("", this, "getMedia", "Error getting Media.", e);
		}
		return null;
	}

	/**
	 * Returns the directory of the specified 'id' file.
	 * 
	 * @param appId
	 * @param id
	 * @param folderType
	 * @param requestType
	 * @return
	 */
	public String getMediaField(String appId, ModelEnum type, String objId, String field) {
		//CACHE
		try {
			return getMedia(appId, type, objId, false).get(field).toString();
		} catch (Exception e) {
			return null;
		}
	}


	// *** DELETE *** //
	
	public Boolean deleteMedia(String appId, ModelEnum type, String objId) {
		//CACHE
		return super.deleteDocument(appId, objId);		
	}

	
	// *** EXISTS *** //
	
	public Boolean mediaExists(String appId, ModelEnum type, String objId) {
		//CACHE
		return super.existsNode(appId, objId);
	}

	// *** OTHERS *** //

}
