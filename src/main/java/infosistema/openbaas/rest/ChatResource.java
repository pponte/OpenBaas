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
import infosistema.openbaas.middleLayer.NotificationMiddleLayer;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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
	private NotificationMiddleLayer noteMid;

	public ChatResource(String appId) {
		this.appId = appId;
		this.sessionMid = SessionMiddleLayer.getInstance();
		this.mediaMid = MediaMiddleLayer.getInstance();
		this.chatMid = ChatMiddleLayer.getInstance();
		this.noteMid = NotificationMiddleLayer.getInstance();
	}

	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response createChatRoom(JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		Date startDate = Utils.getDate();
		String roomName;		
		boolean flag=false;
		JSONArray participants=null;
		Boolean flagNotification=false;
		String sessionToken = Utils.getSessionToken(hh);
		Log.debug("", this, "createChatRoom", "********createChatRoom ************");
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			try {
				roomName = (String) inputJsonObj.opt(ChatRoom.ROOM_NAME);
				participants = (JSONArray) inputJsonObj.get(ChatRoom.PARTICIPANTS);
				flagNotification =  inputJsonObj.optBoolean(ChatRoom.FLAG_NOTIFICATION);
				for(int i = 0; i < participants.length(); i++){
					String userCurr = participants.getString(i);
					if(userCurr.equals(userId))
						flag = true;
				}
				if(!flag) participants.put(userId);
				if(roomName==null){
					roomName = Utils.getStringByJSONArray(participants,";");
				}
				ChatRoom chatRoom = chatMid.createChatRoom(appId, roomName, userId, flagNotification, participants);
				response = Response.status(Status.OK).entity(chatRoom).build();				
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
	@Path("/{chatroomid}/getmessages")
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
		String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			try {
				Long l = (Long)inputJsonObj.get(ChatMessage.DATE);
				date = new Date(l);
				orientation = inputJsonObj.optString(ChatMessage.ORIENTATION);
				numberMessages =  inputJsonObj.optInt(ChatMessage.NUM_MSG);	
				res = chatMid.getMessages(appId,userId,chatRoomId,date,orientation,numberMessages);
				response = Response.status(Status.OK).entity(res).build();
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
	@Path("/{chatroomid}/sendmessage")
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response sendMessage(@PathParam("chatroomid") String chatRoomId, @Context UriInfo ui, @Context HttpHeaders hh,
			@FormDataParam(Const.MESSAGE) String message,
			@FormDataParam(Const.IMAGE) InputStream imageInputStream, 
			@FormDataParam(Const.IMAGE) FormDataContentDisposition imageDetail,
			@FormDataParam(Const.VIDEO) InputStream videoInputStream, 
			@FormDataParam(Const.VIDEO) FormDataContentDisposition videoDetail,
			@FormDataParam(Const.AUDIO) InputStream audioInputStream, 
			@FormDataParam(Const.AUDIO) FormDataContentDisposition audioDetail,
			@FormDataParam(Const.FILE) InputStream fileInputStream, 
			@FormDataParam(Const.FILE) FormDataContentDisposition fileDetail,
			@HeaderParam(value = Const.LOCATION) String location) {
		JSONObject inputJsonObj= new JSONObject();
		if(message!=null){
			try {
				inputJsonObj.put(ChatMessage.MESSAGE_TEXT,URLDecoder.decode(message,"UTF-8"));
			} catch (JSONException e) {
				Log.error("", this, "sendMessage", "Error in message.", e);
				return Response.status(Status.BAD_REQUEST).entity(new Error("Error in message")).build();
			} catch (UnsupportedEncodingException e) {
				Log.error("", this, "sendMessage", "Error in decoding message.", e);
				return Response.status(Status.BAD_REQUEST).entity(new Error("Error in decoding message")).build();
			}
		}
		Response response = null;
		Date startDate = Utils.getDate();
		String fileText;
		String messageText;
		String imageText;
		String audioText;
		String videoText;
		ModelEnum flag = null;
		String sessionToken = Utils.getSessionToken(hh);
		Log.debug("", this, "sendMessage Chat", "********sendMessage Chat************");
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			if(chatMid.existsChatRoom(appId,chatRoomId)){
				try {
					Result res = null;
					if (imageInputStream!=null && imageDetail!=null) {
						res = mediaMid.createMedia(imageInputStream, imageDetail, appId, userId, ModelEnum.image, location, Metadata.getNewMetadata(location));
						flag = ModelEnum.image;
					} else if (videoInputStream!=null && videoDetail!=null) {
						res = mediaMid.createMedia(videoInputStream, videoDetail, appId, userId, ModelEnum.video, location, Metadata.getNewMetadata(location));
						flag = ModelEnum.video;
					} else if (audioInputStream!=null && audioDetail!=null) {
						res = mediaMid.createMedia(audioInputStream, audioDetail, appId, userId, ModelEnum.audio, location, Metadata.getNewMetadata(location));
						flag = ModelEnum.audio;
					} else if (fileInputStream!=null && fileDetail!=null) {
						res = mediaMid.createMedia(fileInputStream, fileDetail, appId, userId, ModelEnum.storage, location, Metadata.getNewMetadata(location));
						flag = ModelEnum.storage;
					}
					if (res!=null && flag!=null) {
						String fileId = ((Media)res.getData()).get_id();
						if(flag.equals(ModelEnum.image)) inputJsonObj.put(ChatMessage.IMAGE_TEXT, fileId);
						if(flag.equals(ModelEnum.storage))inputJsonObj.put(ChatMessage.FILE_TEXT, fileId);
						if(flag.equals(ModelEnum.audio))inputJsonObj.put(ChatMessage.AUDIO_TEXT, fileId);
						if(flag.equals(ModelEnum.video))inputJsonObj.put(ChatMessage.VIDEO_TEXT, fileId);
					}
					imageText = inputJsonObj.optString(ChatMessage.IMAGE_TEXT);
					audioText = inputJsonObj.optString(ChatMessage.AUDIO_TEXT);
					videoText = inputJsonObj.optString(ChatMessage.VIDEO_TEXT);
					fileText = inputJsonObj.optString(ChatMessage.FILE_TEXT);
					messageText = inputJsonObj.optString(ChatMessage.MESSAGE_TEXT);
					ChatMessage msg = chatMid.sendMessage(appId, userId, chatRoomId, fileText, messageText, imageText, audioText, videoText);
					if(msg!=null){
						response = Response.status(Status.OK).entity(msg).build();
						noteMid.setPushNotificationsTODO(appId, userId, chatRoomId, fileText, videoText, imageText, audioText, messageText);
					}else{
						throw new Exception("Error sendMessage");
					}
				} catch (Exception e) {
					Log.error("", this, "sendMessage", "Error sendMessage.", e); 
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error sendMessage").build();
				}
			}else{
				return Response.status(Status.NOT_FOUND).entity("Chat Room not found").build();
			}
		} else if(code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if(code == -1){
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		}
		Date endDate = Utils.getDate();
		Log.info(sessionToken, this, "sendMessage", "Start: " + Utils.printDate(startDate) + " - Finish:" + Utils.printDate(endDate) + " - Time:" + (endDate.getTime()-startDate.getTime()));
		return response;
	}
	
	@POST
	@Path("/{chatroomid}/readmessages")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response readMessages(JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh, 
			@PathParam("chatroomid") String chatRoomId) {
		Response response = null;
		Boolean res = false;
		Date startDate = Utils.getDate();
		String sessionToken = Utils.getSessionToken(hh);
		Log.debug("", this, "readMessages", "********readMessages ************");
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			try {
				JSONArray jsonArray = inputJsonObj.getJSONArray(ChatMessage.MSGSLIST);
				if(jsonArray.length()>0){
					res = chatMid.readMsgsFromUser(appId,userId,jsonArray);
				}
				response = Response.status(Status.OK).entity(res).build();
				noteMid.pushBadge(appId,userId,chatRoomId);
			} catch (Exception e) {
				Log.error("", this, "readMessages", "Error in readMessages.", e);
				return Response.status(Status.BAD_REQUEST).entity("Error parsing the JSON.").build();
			}
		} else if(code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if(code == -1){
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		}
		Date endDate = Utils.getDate();
		Log.info(sessionToken, this, "readMessages", "Start: " + Utils.printDate(startDate) + " - Finish:" + Utils.printDate(endDate) + " - Time:" + (endDate.getTime()-startDate.getTime()));
		return response;
	}
	
	@GET
	@Path("/{chatroomid}/unreadmessages")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response unReadMessages(@Context UriInfo ui, @Context HttpHeaders hh, @PathParam("chatroomid") String chatRoomId) {
		Response response = null;
		List<ChatMessage> lisRes = new ArrayList<ChatMessage>();
		Date startDate = Utils.getDate();
		String sessionToken = Utils.getSessionToken(hh);
		Log.debug("", this, "unReadMessages", "********unReadMessages ************");
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			try {
				lisRes = chatMid.getUnreadMsgs(appId, userId, chatRoomId);
				response = Response.status(Status.OK).entity(lisRes).build();
			} catch (Exception e) {
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
	
}
