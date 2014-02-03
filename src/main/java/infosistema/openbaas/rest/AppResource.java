package infosistema.openbaas.rest;

import infosistema.openbaas.data.Error;
import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.enums.FileMode;
import infosistema.openbaas.data.models.Application;
import infosistema.openbaas.data.models.User;
import infosistema.openbaas.data.models.UsersState;
import infosistema.openbaas.middleLayer.AppsMiddleLayer;
import infosistema.openbaas.middleLayer.UsersMiddleLayer;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;


public class AppResource {

	private AppsMiddleLayer appsMid;
	private UsersMiddleLayer usersMid;
	

	public AppResource() {
		appsMid = AppsMiddleLayer.getInstance();
		usersMid = UsersMiddleLayer.getInstance();
	}

	@Context
	UriInfo uriInfo;

	// *** CREATE *** //

	/**
	 * Create application, fields necessary are: appName. An unique identifier
	 * is generated and returned in the reply.
	 * 
	 * This is the time to define the optional parameters of the app such as if the developer wants to confirm the users
	 * email before allowing them to perform operations, and other relevant options.
	 * 
	 * @param inputJsonObj
	 * @return
	 */
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response createApp(JSONObject inputJsonObj,
			@Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		Log.debug("", this, "post app ", "********post app  ************");
		int code = Utils.treatParametersAdmin(ui, hh);
		if (code == 1) {
			long start = System.currentTimeMillis();
			Application temp = null;
			String appName = null;
			boolean confirmUsersEmail;
			boolean AWS;
			boolean FTP;
			boolean FileSystem;
			String appId = null;
			String appKey = null;
			boolean readSucess = false;
			try {
				appName = (String) inputJsonObj.get(Application.APP_NAME);
				confirmUsersEmail = (boolean) inputJsonObj.optBoolean(Application.CONFIRM_USERS_EMAIL, false);
				AWS = (boolean) inputJsonObj.optBoolean(FileMode.aws.toString(), false);
				FTP = (boolean) inputJsonObj.optBoolean(FileMode.ftp.toString(), false);
				if(!AWS && !FTP)
					FileSystem = (boolean) inputJsonObj.optBoolean(FileMode.filesystem.toString(), true);
				else 
					FileSystem = (boolean) inputJsonObj.optBoolean(FileMode.filesystem.toString(), false);
				if(!AWS && !FTP && !FileSystem)
					FileSystem = true;
				readSucess = true;
			} catch (JSONException e) {
				Log.error("", this, "createApp", "Error parsing the JSON.", e); 
				return Response.status(Status.BAD_REQUEST).entity(new Error("Error parsing the JSON.")).build();
			}
			if (readSucess) {
				appId = Utils.getRandomString(Const.getIdLength());
				appKey = Utils.getRandomString(Const.getIdLength());
				temp = appsMid.createApp(appId, appKey, appName, confirmUsersEmail, AWS, FTP, FileSystem);
			}
			if (temp != null) {
				Result res = new Result(temp, null);
				response = Response.status(Status.CREATED).entity(res).build();
			} else {
				response = Response.status(302).entity(new Error("not created")).build();
			}
			Log.debug("", this, "createApp", "TIME TO FULLFILL REQUEST: " + (System.currentTimeMillis() - start));
		} else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}

	@Path("{appId}/usersstate")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUserState(@PathParam(Const.APP_ID) String appId, JSONObject inputJsonObj, 
			@Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		List<UsersState> listRes = new ArrayList<UsersState>();
		Log.debug("", this, "post getUserState ", "********post getUserState  ************");
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			try {
				JSONArray inputJsonArray = inputJsonObj.getJSONArray(Application.USERS);
				Boolean includeNulls = (Boolean) inputJsonObj.optBoolean(Application.INCLUDEMISSES, false);
				if(inputJsonArray.length()>0){
					for(int i = 0; i < inputJsonArray.length(); i++){
						Object pos = inputJsonArray.get(i);
						if(pos instanceof String){
							String userId = (String) pos;
							Result res = usersMid.getUserInApp(appId, userId);
							User usr = (User)res.getData();
							Metadata meta = (Metadata)res.getMetadata();
							if(usr!=null && meta!=null){
								Boolean online  = Boolean.parseBoolean(usr.getOnline());
								UsersState userState = new UsersState(userId, online, meta.getLastUpdateDate());
								listRes.add(userState);
							}else{
								if(includeNulls)
									listRes.add(null);
							}
						}else{
							if(includeNulls)
								listRes.add(null);
						}
					}
					return Response.status(Status.OK).entity(listRes).build();
				}else{
					return  Response.status(Status.NOT_FOUND).entity(new Error("UserIds Array empty")).build();
				}
			} catch (Exception e) {
				Log.error("", this, "getUserState", "Error in getUserState.", e); 
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Error("Error in getUserState.")).build();
			}
		} else if(code == -2){
			 return Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		 }else if(code == -1)
			 return Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;		
	}
 	// *** UPDATE *** //
	
	/**
	 * Updates the application, optional fields: "newAppName" (new application
	 * name to set), alive (true to reactivate a dead app).
	 * 
	 * @param appId
	 * @param inputJsonObj
	 * @return
	 */
	@Path("{appId}")
	@PATCH
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateApp(@PathParam(Const.APP_ID) String appId,	JSONObject inputJsonObj,
			@Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		Log.debug("", this, "patch app ", "********patch app  ************");
		int code = Utils.treatParametersAdmin(ui, hh);
		if (code == 1) {
			String newAlive = null;
			String newAppName = null;
			Application temp = null;
			Boolean newAWS;
			Boolean newFTP;
			Boolean newFileSystem;
			Boolean newConfirmUsersEmail;
			newAlive = inputJsonObj.optString(Application.ALIVE); //mudar para boolean
			newAppName = inputJsonObj.optString(Application.APP_NAME);
			newConfirmUsersEmail = (boolean) inputJsonObj.optBoolean(Application.CONFIRM_USERS_EMAIL, false);
			newAWS = (Boolean) inputJsonObj.opt(FileMode.aws.toString());
			newFTP = (Boolean) inputJsonObj.opt(FileMode.ftp.toString());
			newFileSystem = (Boolean) inputJsonObj.opt(FileMode.filesystem.toString());
			Application app = this.appsMid.getApp(appId);
			
			
			if(newAlive.equals(""))
				newAlive = app.getAlive();
			if(newAppName.equals(""))
				newAppName = app.getAppName();
			if(newConfirmUsersEmail.equals(""))
				newConfirmUsersEmail = app.getConfirmUsersEmail();
			
			if(newAWS==null)
				newAWS = app.getAWS();
			if(newFTP==null)
				newFTP = app.getFTP();
			if(newFileSystem==null)
				newFileSystem = app.getFileSystem();
			
			if(!newAWS && !newFTP && !newFileSystem)
				newFileSystem = true;
			
			
			if (this.appsMid.appExists(appId)) {
				temp = this.appsMid.updateAllAppFields(appId, newAlive, newAppName,newConfirmUsersEmail,newAWS,newFTP,newFileSystem);
				Result res = new Result(temp, null);
				response = Response.status(Status.OK).entity(res).build();
			} else {
				response = Response.status(Status.NOT_FOUND).entity(new Error(appId)).build();
			}
		}else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}

	
	// *** DELETE *** //
	
	/**
	 * Delete application using its application Identifier.
	 * 
	 * @param appId
	 * @return
	 */
	@Path("{appId}")
	@DELETE
	@Produces({ MediaType.APPLICATION_JSON })
	public Response deleteApp(@PathParam(Const.APP_ID) String appId,
			@Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		Log.debug("", this, "del app ", "********del app  ************");
		int code = Utils.treatParametersAdmin(ui, hh);
		if (code == 1) {
			Log.debug("", this, "deleteApp", "*Deleting App (setting as inactive)*");
			if (this.appsMid.removeApp(appId)) {
				response = Response.status(Status.OK).entity(appId).build();
			} else
				response = Response.status(Status.NOT_FOUND).entity(new Error(appId)).build();
		} else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}

	
	// *** GET LIST *** //


	// *** GET *** //
	
	/**
	 * Get Application Information using its Application Identifier.
	 * 
	 * @param appId
	 * @return
	 */
	@Path("{appId}")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response findById(@PathParam(Const.APP_ID) String appId,
			@Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		int code = Utils.treatParametersAdmin(ui, hh);
		if (code == 1) {
			Log.debug("", this, "findById", "********Finding App info************");
			Application temp = appsMid.getApp(appId);
			if (temp == null)
				return Response.status(Status.NOT_FOUND).entity(new Error("App not exist")).build();
			else {
				Result res = new Result(temp, null);
				response = Response.status(Status.OK).entity(res).build();
			}
		} else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}

	
	// *** OTHERS *** //
	
	// *** RESOURCES *** //

	// This is needed to use patch methods, don't delete it!
	@Target({ ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@HttpMethod("PATCH")
	public @interface PATCH {
	}

	/**
	 * Launches the resource to handle /users requests.
	 * 
	 * @param appId
	 * @return
	 */
	@Path("{appId}/users")
	public UsersResource users(@PathParam(Const.APP_ID) String appId) {
		try {
			return new UsersResource(uriInfo, appId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "users", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

	/**
	 * Launches the resource to handle /media requests.
	 * 
	 * @param appId
	 * @return
	 */
	@Path("{appId}/media")
	public MediaResource media(@PathParam(Const.APP_ID) String appId) {
		try {
			return new MediaResource(appId) ;
		} catch (IllegalArgumentException e) {
			Log.error("", this, "media", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

	/**
	 * Launches the resource to handle /data requests.
	 * 
	 * @param appId
	 * @return
	 */
	@Path("{appId}/data")
	public AppDataResource data(@PathParam(Const.APP_ID) String appId) {
		try {
			return new AppDataResource(uriInfo, appId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "data", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

	/**
	 * Launches the resource to handle /media/audio requests.
	 * 
	 * @param appId
	 * @return
	 */
	@Path("{appId}/media/audio")
	public AudioResource audio(@PathParam(Const.APP_ID) String appId) {
		try {
			return new AudioResource(appId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "audio", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

	/**
	 * Launches the resource to handle /media/video requests.
	 * 
	 * @param appId
	 * @return
	 */
	@Path("{appId}/media/video")
	public VideoResource video(@PathParam(Const.APP_ID) String appId) {
		try {
			return new VideoResource(appId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "video", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

	/**
	 * Launches the resource to handle /media/images requests.
	 * 
	 * @param appId
	 * @return
	 */
	@Path("{appId}/media/images")
	public ImageResource image(@PathParam(Const.APP_ID) String appId) {
		try {
			return new ImageResource(appId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "image", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

	/**
	 * Launches the resource to handle {appID}/storage requests.
	 * 
	 * @param appId
	 * @return
	 */
	@Path("{appId}/storage")
	public StorageResource storage(@PathParam(Const.APP_ID) String appId) {
		try {
			return new StorageResource(appId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "storage", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}
	
	/**
	 * Launches the resource to handle {appID}/account requests.
	 * 
	 * @param appId
	 * @return
	 */
	@Path("{appId}/account")
	public AccountResource account(@PathParam(Const.APP_ID) String appId) {
		try {
			return new AccountResource(appId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "acount", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

}