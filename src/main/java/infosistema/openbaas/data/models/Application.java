package infosistema.openbaas.data.models;

import javax.xml.bind.annotation.XmlRootElement;



@XmlRootElement
public class Application {

	public final static String CREATION_DATE = "creationdate";
	public final static String ALIVE = "alive";
	public final static String APP_NAME = "appName";
	public final static String IMAGE_RES = "imageRes";
	public final static String CONFIRM_USERS_EMAIL = "confirmUsersEmail";
	public final static String UPDATE_DATE = "updateDate";
	public final static String APP_KEY = "appKey";
	public final static String SALT = "salt";
	public final static String HASH = "hash";
	
	
	public final static String INCLUDEMISSES = "includeMisses";
	public final static String USERS = "users";
	 
	private String createdAt;
	private String updatedAt;
	private String _id;
	private String alive;
	private String appName;
	private String appKey;
	private Boolean confirmationEmail;
	private Boolean AWS;
	private Boolean FTP;
	private Boolean FileSystem;
	


	/**
	 * Application constructor with no variables being affected, don't forget to
	 * affect them later.
	 */
	public Application(){
	}
	
	public Application(String _id){
		this._id = _id;
		this.alive = "true";
	}
	public Application(String _id, String date) {
		this._id = _id;
		createdAt = date;
		this.alive = "true";
	}
	public void setCreationDate(String creationDate){
		this.createdAt = creationDate;
	}
	public String getCreationDate() {
		return this.createdAt;
	}

	public String getUpdateDate() {
		return this.updatedAt;
	}
	public void setUpdateDate(String updatedAt){
		this.updatedAt = updatedAt;
	}
	public String getAppId() {
		return this._id;
	}
	public void setAppId(String _id){
		this._id = _id;
	}
	public void setAlive(String alive){
		this.alive = alive;
	}
	public void setInactive(){
		this.alive = "false";
	}
	/**
	 * Gets the application alive field (true -> an app is active, false -> it is not).
	 */
	public String getAlive() {
		return this.alive;
	}
	public String getAppName(){
		return this.appName;
	}
	public void setAppName(String appName){
		this.appName = appName;
	}
	public Boolean getConfirmUsersEmail() {
		return confirmationEmail;
	}
	public void setConfirmUsersEmail(Boolean confirmationEmail) {
		this.confirmationEmail = confirmationEmail;
	}
	
	public Boolean getAWS() {
		return AWS;
	}

	public void setAWS(Boolean aWS) {
		AWS = aWS;
	}

	public Boolean getFTP() {
		return FTP;
	}

	public void setFTP(Boolean fTP) {
		FTP = fTP;
	}

	public Boolean getFileSystem() {
		return FileSystem;
	}

	public void setFileSystem(Boolean fileSystem) {
		FileSystem = fileSystem;
	}

	public String getAppKey() {
		return appKey;
	}

	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}
}
