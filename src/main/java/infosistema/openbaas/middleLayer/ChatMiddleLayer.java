package infosistema.openbaas.middleLayer;

import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.Application;
import infosistema.openbaas.data.models.Certificate;
import infosistema.openbaas.data.models.ChatMessage;
import infosistema.openbaas.data.models.ChatRoom;
import infosistema.openbaas.dataaccess.models.ChatModel;
import infosistema.openbaas.dataaccess.models.NotificationsModel;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;
import infosistema.openbaas.utils.ApplePushNotifications;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javapns.devices.Device;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.DBObject;

public class ChatMiddleLayer extends MiddleLayerAbstract{

	// *** MEMBERS *** //
	private ChatModel chatModel;
	private NotificationsModel noteModel;

	// *** INSTANCE *** //
	private static ChatMiddleLayer instance = null;
	
	private ChatMiddleLayer() {
		super();
		chatModel = new ChatModel();
		noteModel = new NotificationsModel();
	}
	
	public static ChatMiddleLayer getInstance() {
		if (instance == null) instance = new ChatMiddleLayer();
		return instance;
	}
	
	@Override
	protected List<DBObject> getAllSearchResults(String appId, String userId,
			String url, Double latitude, Double longitude, Double radius,
			JSONObject query, String orderType, String orderBy, ModelEnum type,
			List<String> toShow) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public ChatRoom createChatRoom(String appId, String roomName, String userIdCriador,
			Boolean flagNotification,JSONArray participants) {
		ChatRoom res = null;
		String existChatRoomId = null;
		if(participants.length()>1){
			//ordenar JsonArray;
			participants = orderJsonArray(participants);
			if(participants.length()==2){
				existChatRoomId = chatModel.existsChat(Utils.getStringByJSONArray(participants, ";"),appId);
			}
			if(existChatRoomId!=null){
				res = chatModel.getChatRoom(appId, existChatRoomId);
			}else{
				try {
					List<String> unreadUsers = new ArrayList<String>();
					for(int i = 0; i<participants.length();i++){
						String curr = participants.getString(i);
						if(!curr.equals(userIdCriador))
							unreadUsers.add(curr);
					}
					
					String strParticipants = Utils.getStringByJSONArray(participants,";");
					String msgId = "Msg_EMPTY";
					String chatRoomId = "Chat_"+Utils.getRandomString(Const.getIdLength());
					
					ChatMessage msg = new ChatMessage(msgId, new Date(), userIdCriador, "","","","","");
					Boolean msgStorage = chatModel.createMsg(appId, msg);
					Boolean msgRoomStorage = chatModel.createChatRoom(appId, msgId, chatRoomId, roomName, userIdCriador, flagNotification,strParticipants);
					
					if(msgRoomStorage&&msgStorage)
						res = chatModel.getChatRoom(appId, chatRoomId);;
				} catch (Exception e) {
					Log.error("", this, "createChatRoom", "Error ocorred.", e); 
				}
			}
		}else{
			return null;
		}
		return res;
	}

	private JSONArray orderJsonArray(JSONArray participants) {
		JSONArray res = null;
		try {
			String[] array = new String[participants.length()];
			for(int i =0; i<participants.length();i++){
					array[i] = participants.getString(i);	
			}
			Arrays.sort(array);
			res = new JSONArray();
			for(int i =0; i<array.length;i++){
				res.put(array[i]);
			}
		} catch (Exception e) {
			Log.error("", this, "orderJsonArray", "Error ocorred.", e); 
		}
		return res;
	}
/*
	public ChatMessage sendMessage(String appId, String sender, String chatRoomId, String fileText,String messageText,
			String imageText,String audioText, String videoText) {
		ChatMessage res = null;
		List<String> participants = new ArrayList<String>();
		participants = chatModel.getListParticipants(appId, chatRoomId);
		if(participants.size()>0 && participants!=null){
			try {
				Boolean flagNotification = chatModel.hasNotification(appId,chatRoomId);
				Application app = appModel.getApplication(appId);
				List<String> clientsList = app.getClients();
				List<Certificate> certList = new ArrayList<Certificate>();
				if(flagNotification){
					Iterator<String> it2 = clientsList.iterator();
					while(it2.hasNext()){
						String clientId = it2.next();
						certList.add(noteModel.getCertificate(appId,clientId));
					}
				}
				List<String> unReadUsers = new ArrayList<String>();
				Iterator<String> it = participants.iterator();
				while(it.hasNext()){
					String curr = it.next();
					if(!curr.equals(sender)){
						unReadUsers.add(curr);
						if(flagNotification){
							if(app!=null){
								if(clientsList!= null && clientsList.size()>0){
									if(certList.size()>0){
										Iterator<Certificate> it3 = certList.iterator();
										while(it3.hasNext()){
											Certificate certi = it3.next();
											List<Device> devices = noteModel.getDeviceIdList(appId, curr, certi.getClientId());
											if(devices!=null && devices.size()>0){
												int badge = chatModel.getTotalUnreadMsg(appId, curr).size();
												ApplePushNotifications.pushCombineNotification("Recebeu uma mensagem nova",badge,certi.getCertificatePath(), certi.getAPNSPassword(), Const.getAPNS_PROD(), devices);
											}
										}
									}								
								}
							}
						}
					}
				}
				String msgId = "Msg_"+Utils.getRandomString(Const.getIdLength());
				ChatMessage msg = new ChatMessage(msgId, new Date(), sender, messageText, fileText, imageText, audioText, videoText);
				Boolean msgStorage = chatModel.createMsg(appId, msg);
				Boolean addMsgRoom = chatModel.addMsg2Room(appId, msgId, chatRoomId, unReadUsers);
				
				if(addMsgRoom && msgStorage)
					res = msg;
			} catch (Exception e) {
				Log.error("", this, "createChatRoom", "Error parsing the JSON.", e); 
			}
		}else{
			return null;
		}
		return res;
	}*/
	
	public ChatMessage sendMessage(String appId, String sender, String chatRoomId, String fileText,String messageText,
			String imageText,String audioText, String videoText) {
		ChatMessage res = null;
		List<String> participants = new ArrayList<String>();
		participants = chatModel.getListParticipants(appId, chatRoomId);
		if(participants.size()>0 && participants!=null){
			try {
				List<String> unReadUsers = new ArrayList<String>();
				Iterator<String> it = participants.iterator();
				while(it.hasNext()){
					String curr = it.next();
					if(!curr.equals(sender)){
						unReadUsers.add(curr);
					}
				}
				String msgId = "Msg_"+Utils.getRandomString(Const.getIdLength());
				ChatMessage msg = new ChatMessage(msgId, new Date(), sender, messageText, fileText, imageText, audioText, videoText);
				Boolean msgStorage = chatModel.createMsg(appId, msg);
				Boolean addMsgRoom = chatModel.addMsg2Room(appId, msgId, chatRoomId, unReadUsers);
				if(addMsgRoom && msgStorage)
					res = msg;
			} catch (Exception e) {
				Log.error("", this, "createChatRoom", "Error parsing the JSON.", e); 
			}
		}else{
			return null;
		}
		return res;
	}

	public List<ChatMessage> getMessages(String appId, String userId, String chatRoomId, Date date, String orientation, Integer numberMessages) {
		
		List<ChatMessage> res = new ArrayList<ChatMessage>();
		List<String> unreadMsg = new ArrayList<String>();
		try {
			unreadMsg = chatModel.getTotalUnreadMsg(appId, userId);
			res = chatModel.getMessageList(appId,chatRoomId, date, numberMessages, orientation);
			Iterator<ChatMessage> it = res.iterator();
			while(it.hasNext()){
				ChatMessage msg = it.next();
				if(unreadMsg.contains(msg.get_id()))
					msg.setRead(false);
				else
					msg.setRead(true);				
			}
		} catch (Exception e) {
			Log.error("", this, "getMessages", "Error parsing the JSON.", e); 
		}
		return res;
	}

	public Boolean readMsgsFromUser(String appId, String userId, JSONArray jsonArray) {
		Boolean res = false;
		try {
			int num = chatModel.readMessages(appId,userId, jsonArray);
			if(num==jsonArray.length()){
				res =true;
			}
		} catch (Exception e) {
			Log.error("", this, "getMessages", "Error parsing the JSON.", e); 
		}
		return res;
	}

	public Boolean existsChatRoom(String appId, String chatRoomId) {
		Boolean res = false;		
		try {
			res = chatModel.existsKey(appId,chatRoomId);
		} catch (Exception e) {
			Log.error("", this, "getMessages", "Error parsing the JSON.", e); 
		}
		return res;
	}
	
	public List<ChatMessage> getUnreadMsgs(String appId, String userId, String chatRoomId) {
		List<ChatMessage> res = new ArrayList<ChatMessage>();		
		try {
			List<String> msgList = chatModel.getTotalUnreadMsg(appId,userId);
			Iterator<String> it = msgList.iterator();
			while(it.hasNext()){
				String msgId = it.next();
				ChatMessage msg = chatModel.getMsg(appId, msgId);
				res.add(msg);
			}
		} catch (Exception e) {
			Log.error("", this, "getMessages", "Error parsing the JSON.", e); 
		}
		return res;
	}	
	
}
