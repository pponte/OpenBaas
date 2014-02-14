package infosistema.openbaas.rest;

import java.util.Date;

import infosistema.openbaas.data.Error;
import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.models.Application;
import infosistema.openbaas.data.models.User;
//import infosistema.openbaas.dataaccess.models.DocumentModel;
import infosistema.openbaas.middleLayer.AppsMiddleLayer;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.middleLayer.UsersMiddleLayer;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class IntegrationResource {

	
	private UsersMiddleLayer usersMid;
	private String appId;
	private SessionMiddleLayer sessionMid;
	private AppsMiddleLayer appsMid;

	@Context
	UriInfo uriInfo;
	
	public IntegrationResource(String appId) {
		this.usersMid = UsersMiddleLayer.getInstance();
		this.appId = appId;
		this.sessionMid = SessionMiddleLayer.getInstance();
		this.appsMid = AppsMiddleLayer.getInstance();
	}
	
    
	@Path("/test")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response test(JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh) {
		/*
		//Serve para apagar coisas do redis
		
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisGeneralServer(),Const.getRedisGeneralPort());
		//JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(),Const.getRedisSessionPort());
		
		Jedis jedis = pool.getResource();
		try {
			Set<String> a = jedis.keys("app:2be9*");
			Iterator<String> it =  a.iterator();
			while(it.hasNext()){
				String s = it.next();
				jedis.del(s);
				System.out.println(s);
			}
		} finally {
			pool.returnResource(jedis);
		}
		*/
		
		
		
		return Response.status(Status.OK).entity("DEL OK").build();
	}
	
	
	
	
	/**
	 * Creates a user in the application. Necessary fields: "facebook id"
	 * and "email". if the user already register only signin. if not signup
	 * 
	 * @param inputJsonObj
	 * @return
	 */
	@Path("/facebook")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createOrLoginFacebookUser(JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh) {
		Date startDate = Utils.getDate();
		Response response = null;
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		String email = null;
		String name = null;
		String socialNetwork = null;
		String socialId = null;
		//String userSocialId = null;
		String userName = null;
		String userAgent = null;
		String location = null;
		String appKey = null;
		String fbToken = null;
		String lastLocation = null;
		User outUser = new User();
		String userId =null;
		try {
			location = headerParams.getFirst(Const.LOCATION);
		} catch (Exception e) { }
		try {
			userAgent = headerParams.getFirst(Const.USER_AGENT);
		} catch (Exception e) { }
		try {
			appKey = headerParams.getFirst(Application.APP_KEY);
		} catch (Exception e) { }
		if(appKey==null)
			return Response.status(Status.BAD_REQUEST).entity("App Key not found").build();
		if(!appsMid.authenticateApp(appId,appKey))
			return Response.status(Status.UNAUTHORIZED).entity("Wrong App Key").build();
		
		try {
			fbToken = (String) inputJsonObj.get("fbToken");
			JSONObject jsonReqFB = getFBInfo(fbToken);
			if(jsonReqFB == null)
				return Response.status(Status.BAD_REQUEST).entity("Bad FB Token!!!").build();
			email = (String) jsonReqFB.opt("email");
			socialNetwork = "Facebook";
			socialId = (String) jsonReqFB.get("id"); 
			userName = (String) jsonReqFB.opt("username");
			name = (String) jsonReqFB.opt("name");
		} catch (JSONException e) {
			Log.error("", this, "createOrLoginFacebookUser", "Error parsing the JSON.", e); 
			return Response.status(Status.BAD_REQUEST).entity(new Error("Error reading FB info")).build();
		}		
				
		if(email == null && userName != null){
			email = userName+"@facebook.com";
		}
		if(email == null && userName == null){
			email = socialId+"@facebook.com";
			userName = name;
			
		}
		userId = usersMid.getUserIdUsingEmail(appId, email);
		//userSocialId = usersMid.socialUserExists(appId, socialId, socialNetwork);
		if (userId == null) {
			Log.debug("", this, "signup with FB", "********signup with FB ************ email: "+email);
			if (uriInfo == null) uriInfo=ui;
			Result res = usersMid.createSocialUserAndLogin(headerParams, appId, userName,email, socialId, socialNetwork, Metadata.getNewMetadata(location));
			Date endDate = Utils.getDate();
			Log.info(((User)res.getData()).getReturnToken(), this, "signup fb", "Start: " + Utils.printDate(startDate) + " - Finish:" + Utils.printDate(endDate) + " - Time:" + (endDate.getTime()-startDate.getTime()));
			response = Response.status(Status.CREATED).entity(res).build();
		} else {
			Log.debug("", this, "signin with FB", "********signin with FB ************ email: "+email);
			String sessionToken = Utils.getRandomString(Const.getIdLength());
			boolean validation = sessionMid.createSession(sessionToken, appId, userId, socialId);
			if(validation){
				sessionMid.refreshSession(sessionToken, location, userAgent);
				Result data = usersMid.getUserInApp(appId, userId);
				User user = (User) data.getData();
				outUser.setBaseLocation(user.getBaseLocation());
				outUser.setBaseLocationOption(user.getBaseLocationOption());
				if(location!=null){
					if(user.getBaseLocationOption().equals("true")){
						lastLocation = user.getBaseLocation();
					}
					else
						lastLocation = location;
					usersMid.updateUserLocation(userId, appId, lastLocation, Metadata.getNewMetadata(lastLocation));
				}else
					lastLocation = user.getLastLocation();
				outUser.setLastLocation(lastLocation);
				outUser.set_id(userId);
				outUser.setReturnToken(sessionToken);
				outUser.setEmail(email);
				outUser.setUserName(userName);
				Result res = new Result(outUser, data.getMetadata());
				response = Response.status(Status.OK).entity(res).build();
				Date endDate = Utils.getDate();
				Log.info(((User)res.getData()).getReturnToken(), this, "signin fb", "Start: " + Utils.printDate(startDate) + " - Finish:" + Utils.printDate(endDate) + " - Time:" + (endDate.getTime()-startDate.getTime()));
			}
		}
		return response;
	}
	
	private JSONObject getFBInfo(String fbToken) {
		JSONObject res = null;
		try{
			ClientConfig config = new DefaultClientConfig();
			Client client = Client.create(config);
			WebResource service = client.resource(UriBuilder.fromUri("https://graph.facebook.com/me?access_token="+fbToken).build());
			res = new JSONObject(service.accept(MediaType.APPLICATION_JSON).get(String.class));
		}
		catch (Exception e) {
			Log.error("", this, "FB Conn", "FB Conn", e);
		}
		return res;
	}
	

	/**
	 * Creates a user in the application. Necessary fields: "linkedin id".
	 * if the user already register only signin. if not signup
	 * 
	 * @param inputJsonObj
	 * @return
	 */
	@Path("/linkedin")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createOrLoginLinkedInUser(JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		String email = null;
		String socialNetwork = null;
		String socialId = null;
		String userSocialId = null;
		String userName = null;
		String userAgent = null;
		String location = null;
		User outUser = new User();
		String appKey = null;
		String userId =null;
		try {
			location = headerParams.getFirst(Const.LOCATION);
		} catch (Exception e) { }
		try {
			userAgent = headerParams.getFirst(Const.USER_AGENT);
		} catch (Exception e) { }
		try {
			appKey = headerParams.getFirst(Application.APP_KEY);
		} catch (Exception e) { }
		if(appKey==null)
			return Response.status(Status.BAD_REQUEST).entity("App Key not found").build();
		if(!appsMid.authenticateApp(appId,appKey))
			return Response.status(Status.UNAUTHORIZED).entity("Wrong App Key").build();
		try {
			email = (String) inputJsonObj.get("email");
			socialNetwork = "LinkedIn";
			socialId = ((Integer) inputJsonObj.get("socialId")).toString(); 
			userName = (String) inputJsonObj.opt("userName");
			
		} catch (JSONException e) {
			Log.error("", this, "createOrLoginLinkedInUser", "Error parsing the JSON.", e); 
			return Response.status(Status.BAD_REQUEST).entity("Error reading JSON").build();
		}
		if (userName == null) {
			userName = email;
		}
		userId = usersMid.getUserIdUsingEmail(appId, email);
		userSocialId = usersMid.socialUserExists(appId, socialId, socialNetwork);
		
		if(userId!=null && userSocialId==null)
			response =  Response.status(302).entity("User "+userId+" with email: "+email+" already exists in app.").build();
		
		
		if (userId==null) {
			if (uriInfo == null) uriInfo=ui;
			Result res = usersMid.createSocialUserAndLogin(headerParams, appId, userName,email, socialId, socialNetwork, Metadata.getNewMetadata(location));
			response = Response.status(Status.CREATED).entity(res).build();
		} else {
			String sessionToken = Utils.getRandomString(Const.getIdLength());
			boolean validation = sessionMid.createSession(sessionToken, appId, userId, socialId);
			if(validation){
				sessionMid.refreshSession(sessionToken, location, userAgent);
				outUser.set_id(userId);
				outUser.setReturnToken(sessionToken);
				outUser.setEmail(email);
				outUser.setUserName(userName);
				Result res = new Result(outUser, null);
				response = Response.status(Status.OK).entity(res).build();
			}
		}
		return response;
	}
}
