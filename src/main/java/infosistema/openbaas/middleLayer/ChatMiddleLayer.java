package infosistema.openbaas.middleLayer;

import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.ChatMessage;
import infosistema.openbaas.dataaccess.models.ChatModel;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.DBObject;

public class ChatMiddleLayer extends MiddleLayerAbstract{

	// *** MEMBERS *** //
	private ChatModel chatModel;


	// *** INSTANCE *** //
	private static ChatMiddleLayer instance = null;
	
	private ChatMiddleLayer() {
		super();
		chatModel = new ChatModel();
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

	public String createChatRoom(String appId, String roomName, String userIdCriador,Boolean flagNotification,
			String fileText, String messageText, JSONArray participants) {
		String res = null;
		if(participants.length()>0){
			try {
				List<String> unreadUsers = new ArrayList<String>();
				for(int i = 0; i<participants.length();i++){
					String curr = participants.getString(i);
					if(!curr.equals(userIdCriador))
						unreadUsers.add(curr);
				}
				
				String strParticipants = Utils.getStringByJSONArray(participants,";");
				String msgId = "Msg_"+Utils.getRandomString(Const.getIdLength());
				String chatRoomId = "Chat_"+Utils.getRandomString(Const.getIdLength());
				
				ChatMessage msg = new ChatMessage(msgId, new Date(), userIdCriador, messageText,fileText);
				Boolean msgStorage = chatModel.createMsg(appId, msg);
				Boolean msgRoomStorage = chatModel.addStartMsg2Room(appId, msgId, chatRoomId, roomName, userIdCriador, flagNotification,strParticipants,unreadUsers);
				
				if(msgRoomStorage&&msgStorage)
					res = chatRoomId;
			} catch (Exception e) {
				Log.error("", this, "createChatRoom", "Error parsing the JSON.", e); 
			}
		}else{
			return null;
		}
		return res;
	}

	public ChatMessage sendMessage(String appId, String sender, String chatRoomId, String fileText,String messageText) {
		ChatMessage res = null;
		List<String> participants = new ArrayList<String>();
		participants = chatModel.getListParticipants(appId, chatRoomId);
		if(participants.size()>0 && participants!=null){
			try {
				List<String> unReadUsers = new ArrayList<String>();
				Iterator<String> it = participants.iterator();
				while(it.hasNext()){
					String curr = it.next();
					if(!curr.equals(sender))
						unReadUsers.add(curr);
				}
				String msgId = "Msg_"+Utils.getRandomString(Const.getIdLength());
				ChatMessage msg = new ChatMessage(msgId, new Date(), sender, messageText,fileText);
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

	public List<ChatMessage> getMessages(String appId, String chatRoomId, Date date, String orientation, Integer numberMessages) {
		List<ChatMessage> res = new ArrayList<ChatMessage>();
		try {
			res = chatModel.getMessageList(appId,chatRoomId, date, numberMessages, orientation);
		} catch (Exception e) {
			Log.error("", this, "getMessages", "Error parsing the JSON.", e); 
		}
		return res;
	}	
}
