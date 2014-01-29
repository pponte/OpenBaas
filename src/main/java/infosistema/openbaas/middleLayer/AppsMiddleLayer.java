package infosistema.openbaas.middleLayer;

import java.util.Date;
import java.util.HashMap;


import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.identitymanagement.model.EntityAlreadyExistsException;

import infosistema.openbaas.data.models.Application;
import infosistema.openbaas.dataaccess.files.FileInterface;
import infosistema.openbaas.dataaccess.models.AppModel;
import infosistema.openbaas.dataaccess.models.MediaModel;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.encryption.PasswordEncryptionService;

public class AppsMiddleLayer extends MiddleLayerAbstract {

	// *** MEMBERS *** //

	AppModel appModel = new AppModel();	
	MediaModel mediaModel = new MediaModel();	
	// *** INSTANCE *** //
	
	private static AppsMiddleLayer instance = null;

	public static AppsMiddleLayer getInstance() {
		if (instance == null) instance = new AppsMiddleLayer();
		return instance;
	}
	
	private AppsMiddleLayer() {
		super();
	}

	// *** CREATE *** //
	
	/**
	 * returns true if created Application sucessfully.
	 * 
	 * @param appId
	 * @param appName
	 * @return
	 */
	public Application createApp(String appId, String appKey, String appName, boolean userEmailConfirmation,
			boolean AWS,boolean FTP,boolean FileSystem) {
		byte[] salt = null;
		byte[] hash = null;
		PasswordEncryptionService service = new PasswordEncryptionService();
		Application app = null;
		try {
			salt = service.generateSalt();
			hash = service.getEncryptedPassword(appKey, salt);
			app = appModel.createApp(appId,appKey, hash, salt, appName, new Date().toString(), userEmailConfirmation,AWS,FTP,FileSystem);
		} catch (Exception e) {
			Log.error("", this, "createApp Login","", e); 
		}
		return app;
	}

	public boolean createAppFileSystem(String appId) {
		FileInterface fileModel = getAppFileInterface(appId);
		try{
			return fileModel.createApp(appId);
		} catch(EntityAlreadyExistsException e) {
			Log.error("", this, "createAppFileSystem", "Entity Already Exists.", e); 
		} catch(AmazonServiceException e) {
			Log.error("", this, "createAppFileSystem", "Amazon Service error.", e); 
		}catch(Exception e) {
			Log.error("", this, "createAppFileSystem", "An error ocorred.", e); 
		}
		return false;
	}


	// *** UPDATE *** //
	
	public Application updateAllAppFields(String appId, String alive, String newAppName, boolean confirmUsersEmail,boolean AWS,boolean FTP,boolean FILESYSTEM) {
		if (appModel.appExists(appId)) {
			appModel.updateAppFields(appId, alive, newAppName, confirmUsersEmail,AWS,FTP,FILESYSTEM);
			return appModel.getApplication(appId);
		}
		return null;
	}


	// *** DELETE *** //
	
	public boolean removeApp(String appId) {
		return appModel.deleteApp(appId);
	}


	// *** GET LIST *** //

	// *** GET *** //
	
	public Application getApp(String appId) {	
		Application temp = new Application(appId);
		temp = appModel.getApplication(appId);
		return temp;
	}
	
	public HashMap<String, String> getAuthApp(String appId) {	
		HashMap<String, String> temp = new HashMap<String, String>();
		temp = appModel.getApplicationAuth(appId);
		return temp;
	}


	// *** EXISTS *** //

	public boolean appExists(String appId) {
		return appModel.appExists(appId);
	}

	
	// *** OTHERS *** //
	
	public void reviveApp(String appId){
		appModel.reviveApp(appId);
	}

	public Boolean authenticateApp(String appId, String appKey) {
		try {
			HashMap<String, String> fieldsAuth = getAuthApp(appId);
			byte[] salt = null;
			byte[] hash = null;
			if(fieldsAuth.containsKey(Application.HASH) && fieldsAuth.containsKey(Application.SALT)){
				salt = fieldsAuth.get(Application.SALT).getBytes("ISO-8859-1");
				hash = fieldsAuth.get(Application.HASH).getBytes("ISO-8859-1");
			}
			PasswordEncryptionService service = new PasswordEncryptionService();
			Boolean authenticated = false;
			authenticated = service.authenticate(appKey, hash, salt);
			return authenticated;
		} catch (Exception e) {
			Log.error("", "", "authenticateAPP", "", e); 
		} 	
		return false;
	}

}
