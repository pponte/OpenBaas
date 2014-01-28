package infosistema.openbaas.middleLayer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.sun.jersey.core.header.FormDataContentDisposition;

import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.Audio;
import infosistema.openbaas.data.models.Image;
import infosistema.openbaas.data.models.Media;
import infosistema.openbaas.data.models.Storage;
import infosistema.openbaas.data.models.Video;
import infosistema.openbaas.dataaccess.files.FileInterface;
import infosistema.openbaas.dataaccess.models.MediaModel;
import infosistema.openbaas.dataaccess.models.ModelAbstract;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;


public class MediaMiddleLayer extends MiddleLayerAbstract {

	// *** MEMBERS *** //

	private MediaModel mediaModel;


	// *** INSTANCE *** //
	
	private static MediaMiddleLayer instance = null;
	
	private MediaMiddleLayer() {
		super();
		mediaModel = new MediaModel();
	}
	
	public static MediaMiddleLayer getInstance() {
		if (instance == null) instance = new MediaMiddleLayer();
		return instance;
	}


	// *** PRIVATE *** //

	private String createFileId(String appId, ModelEnum type) {
		String id = Utils.getRandomString(Const.getIdLength());
		while (mediaModel.mediaExists(appId, type, id)) {
			id = Utils.getRandomString(Const.getIdLength());
		}
		return type+":"+id;
	}
	
	private Map<String, String> getFileFields(InputStream stream, FormDataContentDisposition fileDetail,
			String location, ModelEnum type) {
		
		String fullFileName = fileDetail.getFileName();
		int idx = fullFileName.lastIndexOf(".");
		String fileName = (idx < 0 ? fullFileName : fullFileName.substring(0, idx));
		String fileExtension = (idx < 0 ? "" : fullFileName.substring(idx + 1));
		String fileSize = "-1";
		Map<String, String> fields = new HashMap<String, String>();

		fields.put(Media.SIZE, fileSize);
		fields.put(Media.FILENAME, fileName);
		fields.put(Media.FILEEXTENSION, fileExtension);
		fields.put(Media.LOCATION, location);
		//TODO: SACAR do STREAM A INFORMAÇÃO AQUI A BAIXO
		if (type == ModelEnum.audio) {
			fields.put(Audio.BITRATE, Const.getAudioDegaultBitrate());
		} else if (type == ModelEnum.image) {
			fields.put(Image.SIZE, fileSize);
			fields.put(Image.RESOLUTION, Const.getImageDefaultSize());
			fields.put(Image.PIXELSIZE, Const.getImageDefaultSize());
		} else if (type == ModelEnum.video) {
			fields.put(Video.RESOLUTION, Const.getVideoDefaultResolution());
		} else if (type == ModelEnum.storage) {
		}
		return fields;
	}

	
	// *** CREATE *** //

	public Result createMedia(InputStream stream, FormDataContentDisposition fileDetail, String appId, String userId,
			ModelEnum type, String location, Map<String, String> extraMetadata) {

		String id = createFileId(appId, type);
		
		///// OLD
		String filePath = "";
		///// OLD /////

		// Get data from file
		Map<String, String> fields = getFileFields(stream, fileDetail, location, type);
		
		//Upload File
		FileInterface fileModel = getAppFileInterface(appId);
		try{
			filePath = fileModel.upload(appId, type, id, fields.get(Media.FILEEXTENSION), stream);
			File file = new File(filePath);
			fields.put(Media.PATH, filePath);
			fields.put(type+"Id", id);
			fields.put(Media.SIZE, String.valueOf(file.length()));
			
		} catch(AmazonServiceException e) {
			Log.error("", this, "upload", "Amazon Service error.", e);
			return null;
		} catch(AmazonClientException e) {
			Log.error("", this, "upload", "Amazon Client error.", e); 
			return null;
		} catch(Exception e) {
			Log.error("", this, "upload", "An error ocorred.", e); 
			return null;
		}
		if (location != null){
			String[] splitted = location.split(":");
			geo.insertObjectInGrid(Double.parseDouble(splitted[0]),	Double.parseDouble(splitted[1]), type, appId, id);
		}
		
		Metadata metadata = null;
		Object data = null;
		data = mediaModel.createMedia(appId, userId, type, id, fields, extraMetadata);
		if (data != null) {
			try {
				metadata = Metadata.getMetadata(((JSONObject) data).getJSONObject(ModelAbstract._METADATA));
			} catch (JSONException e) {
				Log.error("", this, "createMedia", "Error gettin metadata.", e);
			}
			((JSONObject) data).remove(ModelAbstract._METADATA);
			data = (DBObject)JSON.parse(data.toString());
			return new Result(data, metadata);
		} else {
			return null;
		}
	}


	// *** UPDATE *** //
	
	// *** DELETE *** //
	
	public boolean deleteMedia(String appId, ModelEnum type, String id) {
		String extension = mediaModel.getMediaField(appId, type, id, Media.FILEEXTENSION);
		String location = mediaModel.getMediaField(appId, type, id, Media.LOCATION);
		FileInterface fileModel = getAppFileInterface(appId);
		Boolean res = false;
		try{
			res = fileModel.deleteFile(appId, type, id, extension);
			
		}catch(NoSuchEntityException e){
			Log.error("", this, "deleteFile", "No such element error.", e); 
		}
		res = mediaModel.deleteMedia(appId, type, id);
				
		if (location != null){
			String[] splitted = location.split(":");
			geo.deleteObjectFromGrid(Double.parseDouble(splitted[0]),	Double.parseDouble(splitted[1]), type, appId, id);
		}
		
		return res ;
	}


	// *** GET LIST *** //

	@Override
	protected List<String> getAllSearchResults(String appId, String userId, String url, JSONObject query, String orderType, ModelEnum type) throws Exception {
		if(query==null){
			query = new JSONObject();
			JSONObject jAux= new JSONObject();
			jAux.put("$exists",1);
			query.put("fileExtension", jAux); 
			query.put("imageId", jAux);
			query.put("pixelsSize", jAux);
			query.put("fileName", jAux); 
		}
		return docModel.getDocuments(appId, userId, url, query, orderType);
		//return mediaModel.getMedia(appId, type, query, orderType);
	}
	
	
	// *** GET *** //
	public Result getMedia(String appId, ModelEnum type, String id, boolean getMetadata) {
		JSONObject obj = mediaModel.getMedia(appId, type, id, getMetadata);

		Media media = null;
		Metadata metadata = null;
		
		try {
			obj.put(Media.ID, id);
			if (type == ModelEnum.audio) {
				media = new Audio();
				//((Audio)media).setDefaultBitRate(obj.get(Audio.BITRATE));
			} else if (type == ModelEnum.image) {
				media = new Image();
				//((Image)media).setResolution(obj.get(Image.RESOLUTION));
			} else if (type == ModelEnum.storage) {
				media = new Storage();
			} else if (type == ModelEnum.video) {
				media = new Video();
				//((Video)media).setResolution(obj.get(Video.RESOLUTION));
			}
			media.setId(obj.getString(Media.ID));
			media.setSize(obj.getLong(Media.SIZE));
			media.setDir(obj.getString(Media.PATH));
			media.setFileName(obj.getString(Media.FILENAME));
			media.setFileExtension(obj.getString(Media.FILEEXTENSION));
			media.setLocation(obj.getString(Media.LOCATION));
			if (getMetadata) {
				metadata = Metadata.getMetadata(obj.getJSONObject(ModelAbstract._METADATA));
			}
		} catch (JSONException e) {
			Log.error("", this, "getMedia", "Error getting Media.", e);
		}

		return new Result(media, metadata);

	}

	
	// *** DOWNLOAD *** //

	public byte[] download(String appId, ModelEnum type, String id,String ext) {
		FileInterface fileModel = getAppFileInterface(appId);
		try {
			return fileModel.download(appId, type, id,ext);
		} catch (IOException e) {
			Log.error("", this, "download", "An error ocorred.", e); 
		}
		return null;
	}


	// *** EXISTS *** //

	public boolean mediaExists(String appId, ModelEnum type, String id) {
		return mediaModel.mediaExists(appId, type, id);
	}


	// *** OTHERS *** //

}
