package infosistema.openbaas.rest;

import infosistema.openbaas.data.Error;
import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.ChatMessage;
import infosistema.openbaas.data.models.ChatRoom;
import infosistema.openbaas.data.models.Media;
import infosistema.openbaas.middleLayer.ChatMiddleLayer;
import infosistema.openbaas.middleLayer.MediaMiddleLayer;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;


public class ChatResource {
	
	private String appId;
	private SessionMiddleLayer sessionMid;
	private MediaMiddleLayer mediaMid;
	private ChatMiddleLayer chatMid;

	public ChatResource(String appId) {
		this.appId = appId;
		this.sessionMid = SessionMiddleLayer.getInstance();
		this.mediaMid = MediaMiddleLayer.getInstance();
		this.chatMid = ChatMiddleLayer.getInstance();
	}

	@POST
	@Path("/chatroom")
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response createChatRoom(
			@FormDataParam("inputJson") String json,
			@Context UriInfo ui, @Context HttpHeaders hh,
			@FormDataParam(Const.FILE) InputStream uploadedInputStream,
			@FormDataParam(Const.FILE) FormDataContentDisposition fileDetail, 
			@HeaderParam(value = Const.LOCATION) String location) {
		
		JSONObject inputJsonObj=null;
		String jsonRes = URLDecoder.decode(json);
		try {
			inputJsonObj = new JSONObject(jsonRes);
		} catch (JSONException e1) {
			return Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		}
		Response response = null;
		Date startDate = Utils.getDate();
		String roomName;
		String fileText;
		String messageText;
		
		JSONArray participants=null;
		Boolean flagNotification=true;
		String sessionToken = Utils.getSessionToken(hh);
		Log.debug("", this, "createChatRoom", "********createChatRoom ************");
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			try {
				
				if(uploadedInputStream!=null && fileDetail!=null){
					Result res = mediaMid.createMedia(uploadedInputStream, fileDetail, appId, userId, ModelEnum.image, location, Metadata.getNewMetadata(location));
					String fileId = ((Media)res.getData()).get_id();
					inputJsonObj.put("fileText", fileId);
					createChatRoom(json, ui, hh,null,null,null);
				}
				roomName = (String) inputJsonObj.opt(ChatRoom.ROOM_NAME);
				participants = (JSONArray) inputJsonObj.get(ChatRoom.PARTICIPANTS);
				fileText = inputJsonObj.optString(ChatMessage.FILE_TEXT);
				messageText = inputJsonObj.optString(ChatMessage.MESSAGE_TEXT);
				if(roomName==null){
					roomName = Utils.getStringByJSONArray(participants,";");
				}
				String chatRoomId = chatMid.createChatRoom(appId,roomName,userId,flagNotification,fileText,messageText,participants);
				response = Response.status(Status.CREATED).entity(chatRoomId).build();				
			} catch (Exception e) {
				Log.error("", this, "createChatRoom", "Error creating chat.", e); 
				return Response.status(Status.BAD_REQUEST).entity("Error parsing the JSON.").build();
			}
		} else if(code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if(code == -1){
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		}
		Date endDate = Utils.getDate();
		Log.info(sessionToken, this, "createChatRoom", "Start: " + Utils.printDate(startDate) + " - Finish:" + Utils.printDate(endDate) + " - Time:" + (endDate.getTime()-startDate.getTime()));
		return response;
	}
	
	@POST
	@Path("/chatroom/{chatroomid}/getmessages")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getMessages(JSONObject inputJsonObj, @Context UriInfo ui, 
			@Context HttpHeaders hh, @PathParam("chatroomid") String chatRoomId) {
		Response response = null;
		Date startDate = Utils.getDate();
		Date date;
		String orientation;
		Integer numberMessages;
		List<ChatMessage> res = new ArrayList<ChatMessage>();
		String sessionToken = Utils.getSessionToken(hh);
		Log.debug("", this, "getMessages", "********getMessages ************");
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		//String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			try {
				Long l = (Long)inputJsonObj.get(ChatMessage.DATE);
				date = new Date(l);
				orientation = inputJsonObj.optString(ChatMessage.ORIENTATION);
				numberMessages =  inputJsonObj.optInt(ChatMessage.NUM_MSG);	
				res = chatMid.getMessages(appId,chatRoomId,date,orientation,numberMessages);
				response = Response.status(Status.CREATED).entity(res).build();
			} catch (JSONException e) {
				Log.error("", this, "createUserAndLogin", "Error parsing the JSON.", e); 
				return Response.status(Status.BAD_REQUEST).entity("Error parsing the JSON.").build();
			}
		} else if(code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if(code == -1){
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		}
		Date endDate = Utils.getDate();
		Log.info(sessionToken, this, "getMessages", "Start: " + Utils.printDate(startDate) + " - Finish:" + Utils.printDate(endDate) + " - Time:" + (endDate.getTime()-startDate.getTime()));
		return response;
	}

	@POST
	@Path("/chatroom/{chatroomid}/sendmessage")
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response sendMessage(@FormDataParam("inputJson") String json, @Context UriInfo ui, @Context HttpHeaders hh,
			@FormDataParam(Const.FILE) InputStream uploadedInputStream, @PathParam("chatroomid") String chatRoomId,
			@FormDataParam(Const.FILE) FormDataContentDisposition fileDetail, 
			@HeaderParam(value = Const.LOCATION) String location) {
		JSONObject inputJsonObj=null;
		String jsonRes = URLDecoder.decode(json);
		try {
			inputJsonObj = new JSONObject(jsonRes);
		} catch (JSONException e1) {
			return Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		}
		Response response = null;
		Date startDate = Utils.getDate();
		String fileText;
		String messageText;
		String sessionToken = Utils.getSessionToken(hh);
		Log.debug("", this, "createChatRoom", "********createChatRoom ************");
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			try {
				if(uploadedInputStream!=null && fileDetail!=null){
					Result res = mediaMid.createMedia(uploadedInputStream, fileDetail, appId, userId, ModelEnum.image, location, Metadata.getNewMetadata(location));
					String fileId = ((Media)res.getData()).get_id();
					inputJsonObj.put(ChatMessage.FILE_TEXT, fileId);
					sendMessage(json, ui, hh,null,chatRoomId,null,null);
				}
				fileText = inputJsonObj.optString(ChatMessage.FILE_TEXT);
				messageText = inputJsonObj.optString(ChatMessage.MESSAGE_TEXT);
				ChatMessage msg = chatMid.sendMessage(appId,userId,chatRoomId,fileText,messageText);
				response = Response.status(Status.CREATED).entity(msg).build();
			} catch (Exception e) {
				Log.error("", this, "createChatRoom", "Error parsing the JSON.", e); 
				return Response.status(Status.BAD_REQUEST).entity("Error parsing the JSON.").build();
			}
		} else if(code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if(code == -1){
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		}
		Date endDate = Utils.getDate();
		Log.info(sessionToken, this, "createChatRoom", "Start: " + Utils.printDate(startDate) + " - Finish:" + Utils.printDate(endDate) + " - Time:" + (endDate.getTime()-startDate.getTime()));
		return response;
	}
	
}
