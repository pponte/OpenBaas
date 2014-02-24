package infosistema.openbaas.dataaccess.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import infosistema.openbaas.data.models.ChatMessage;
import infosistema.openbaas.data.models.ChatRoom;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class ChatModel {

	// request types
		private JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisChatServer(),Const.getRedisChatPort());
		Jedis jedis;
		
		public ChatModel() {
			jedis = new Jedis(Const.getRedisChatServer(), Const.getRedisChatPort());
		}

		// *** PRIVATE *** //
		/*
		private static final String APP_DATA_COLL_FORMAT = "app%sdata";
			
		private static final String USER_FIELD_KEY_FORMAT = "app:%s:user:%s:%s";
		*/
		private static final String SEPARATOR1= ":";		
		private static final String SEPARATOR2 = "_";
		private static final String SEPARATOR3= ";";
		private static final int MAXELEMS = 9999999;
		

		public Boolean createMsg(String appId, ChatMessage msg) {
			Boolean res = false;
			Jedis jedis = pool.getResource();
			long milliseconds = msg.getDate().getTime();
			try {
				jedis.hset(appId+SEPARATOR1+msg.get_id(), ChatMessage._ID, msg.get_id());
				jedis.hset(appId+SEPARATOR1+msg.get_id(), ChatMessage.SENDER, msg.getSender());
				jedis.hset(appId+SEPARATOR1+msg.get_id(), ChatMessage.DATE, String.valueOf(milliseconds));
				try{jedis.hset(appId+SEPARATOR1+msg.get_id(), ChatMessage.MESSAGE_TEXT, msg.getMessageText());}catch(Exception e){}
				try{jedis.hset(appId+SEPARATOR1+msg.get_id(), ChatMessage.FILE_TEXT, msg.getFileId());}catch(Exception e){}
				try{jedis.hset(appId+SEPARATOR1+msg.get_id(), ChatMessage.AUDIO_TEXT, msg.getAudioId());}catch(Exception e){}
				try{jedis.hset(appId+SEPARATOR1+msg.get_id(), ChatMessage.VIDEO_TEXT, msg.getVideoId());}catch(Exception e){}
				try{jedis.hset(appId+SEPARATOR1+msg.get_id(), ChatMessage.IMAGE_TEXT, msg.getImageId());}catch(Exception e){}
				res = true;
			} finally {
				pool.returnResource(jedis);
			}
			return res;
		}
		
		public ChatMessage getMsg(String appId, String msgId) {
			ChatMessage res = new ChatMessage();
			Jedis jedis = pool.getResource();
			try {
				String sender = jedis.hget(appId+SEPARATOR1+msgId, ChatMessage.SENDER);
				String messageText = jedis.hget(appId+SEPARATOR1+msgId, ChatMessage.MESSAGE_TEXT);
				String fileText = jedis.hget(appId+SEPARATOR1+msgId, ChatMessage.FILE_TEXT);
				String audioText = jedis.hget(appId+SEPARATOR1+msgId, ChatMessage.AUDIO_TEXT);
				String videoText = jedis.hget(appId+SEPARATOR1+msgId, ChatMessage.VIDEO_TEXT);
				String imageText = jedis.hget(appId+SEPARATOR1+msgId, ChatMessage.IMAGE_TEXT);
				
				if(sender!=null) res.setSender(sender);
				if(fileText!=null) res.setFileId(fileText);
				if(messageText!=null) res.setMessageText(messageText);
				if(audioText!=null) res.setAudioId(audioText);
				if(videoText!=null) res.setVideoId(videoText);
				if(imageText!=null) res.setImageId(imageText);
				long l = Long.valueOf(jedis.hget(appId+SEPARATOR1+msgId, ChatMessage.DATE)).longValue();
				res.setDate(new Date(l));
				res.set_id(msgId);
			} finally {
				pool.returnResource(jedis);
			}
			return res;
		}
		
		public Boolean createChatRoom(String appId, String msgId,String roomId, String roomName, String roomCreator, Boolean flagNotification, String totalParticipants) {
			Boolean res = false;
			Jedis jedis = pool.getResource();
			if(flagNotification==null) flagNotification = false;
			Long milliseconds = new Date().getTime();
			String[] participants = totalParticipants.split(SEPARATOR3);
			try {
				jedis.hset(appId+SEPARATOR1+roomId, ChatRoom._ID, roomId);
				jedis.hset(appId+SEPARATOR1+roomId, ChatRoom.ROOM_NAME, roomName);
				jedis.hset(appId+SEPARATOR1+roomId, ChatRoom.ROOM_CREATOR, roomCreator);
				jedis.hset(appId+SEPARATOR1+roomId, ChatRoom.FLAG_NOTIFICATION, flagNotification.toString());
				jedis.hset(appId+SEPARATOR1+roomId, ChatRoom.PARTICIPANTS, totalParticipants);
				jedis.hset(appId+SEPARATOR1+roomId, ChatRoom.CREATEDDATE, milliseconds.toString());
				jedis.rpush(appId+SEPARATOR2+roomId, msgId);
				if(participants.length==2)
					jedis.set(appId+SEPARATOR1+totalParticipants, roomId);
				res = true;
			} finally {
				pool.returnResource(jedis);
			}
			return res;
		}
		
		public ChatRoom getChatRoom(String appId, String roomId) {
			ChatRoom res = new ChatRoom();;
			Jedis jedis = pool.getResource();
			try {
				res.set_id(roomId);
				res.setRoomName(jedis.hget(appId+SEPARATOR1+roomId, ChatRoom.ROOM_NAME));
				res.setRoomCreator(jedis.hget(appId+SEPARATOR1+roomId, ChatRoom.ROOM_CREATOR));
				res.setFlagNotification(Boolean.parseBoolean(jedis.hget(appId+SEPARATOR1+roomId, ChatRoom.FLAG_NOTIFICATION)));
				res.setParticipants(jedis.hget(appId+SEPARATOR1+roomId, ChatRoom.PARTICIPANTS).split(SEPARATOR3));
				long l = Long.valueOf(jedis.hget(appId+SEPARATOR1+roomId, ChatRoom.CREATEDDATE)).longValue();
				res.setCreatedDate(new Date(l));
				
			} finally {
				pool.returnResource(jedis);
			}
			return res;
		}
		
		public Boolean addMsg2Room(String appId, String msgId,String roomId, List<String> unReadUsers) {
			Boolean res = false;
			Jedis jedis = pool.getResource();
			Iterator<String> it = unReadUsers.iterator();
			try {
				jedis.rpush(appId+SEPARATOR2+roomId, msgId);
				while(it.hasNext()){
					jedis.rpush(appId+SEPARATOR2+"UnRead"+SEPARATOR2+it.next(), msgId);
				}
				res = true;
			} finally {
				pool.returnResource(jedis);
			}
			return res;
		}

		public List<String> getListParticipants(String appId, String roomId) {
			List<String> res = null;
			Jedis jedis = pool.getResource();
			try {
				String strParticipants = jedis.hget(appId+SEPARATOR1+roomId, ChatRoom.PARTICIPANTS);
				res = Utils.getListByString(strParticipants, ChatRoom.SEPARATOR);
			} finally {
				pool.returnResource(jedis);
			}
			return res;
		}

		public List<ChatMessage> getMessageList(String appId, String roomId, Date date, Integer numberMessages, String orientation) {
			Jedis jedis = pool.getResource();
			List<ChatMessage> res = new ArrayList<ChatMessage>();
			if(orientation==null) orientation = "";
			if(numberMessages==null) numberMessages = 10;
			try {
				Long size = jedis.llen(appId+SEPARATOR2+roomId);		
				Integer endIndex = (int) (long) size;
				Integer startIndex = 1;
				int i = 0;
				int index = (int) (long)size;
				while(i<size){
					index = ((int)(long) (size))-i-1;
					i++;
					String msgIdCurr = jedis.lindex(appId+SEPARATOR2+roomId, index);
					long l = Long.valueOf(jedis.hget(appId+SEPARATOR1+msgIdCurr, ChatMessage.DATE)).longValue();
					Date dateCurr = new Date(l);
					if(dateCurr.compareTo(date)==0 && orientation.equals("front")){
						index -= 1;
						break;
					}
					if(dateCurr.compareTo(date)==0 && !orientation.equals("front")){
						//index += 1;
						break;
					}
					if(dateCurr.compareTo(date)<0)
						break;
				}
				if(orientation.equals("front")){
					startIndex = index+1;
					if(index+numberMessages<endIndex)
						endIndex= index+numberMessages;
				}else{
					endIndex = index;
					startIndex = index-numberMessages+1;
					if(startIndex<1)
						startIndex=1;
				}
				if(startIndex>endIndex){
					return new ArrayList<ChatMessage>();
				}
				for(int o=startIndex;o<=endIndex;o++){
					String msgId = jedis.lindex(appId+SEPARATOR2+roomId, o);
					ChatMessage msg = new ChatMessage();
					String sender = jedis.hget(appId+SEPARATOR1+msgId, ChatMessage.SENDER);
					String messageText = jedis.hget(appId+SEPARATOR1+msgId, ChatMessage.MESSAGE_TEXT);
					String fileText = jedis.hget(appId+SEPARATOR1+msgId, ChatMessage.FILE_TEXT);
					String audioText = jedis.hget(appId+SEPARATOR1+msgId, ChatMessage.AUDIO_TEXT);
					String videoText = jedis.hget(appId+SEPARATOR1+msgId, ChatMessage.VIDEO_TEXT);
					String imageText = jedis.hget(appId+SEPARATOR1+msgId, ChatMessage.IMAGE_TEXT);
					
					if(sender!=null) msg.setSender(sender);
					if(fileText!=null) msg.setFileId(fileText);
					if(messageText!=null) msg.setMessageText(messageText);
					if(audioText!=null) msg.setAudioId(audioText);
					if(videoText!=null) msg.setVideoId(videoText);
					if(imageText!=null) msg.setImageId(imageText);
					long l = Long.valueOf(jedis.hget(appId+SEPARATOR1+msgId, ChatMessage.DATE)).longValue();
					msg.setDate(new Date(l));
					msg.set_id(msgId);
					res.add(msg);
				}
			}catch(Exception e){
				Log.error("", this, "getMessageList", "Error getMessageList redis.", e); 
			} finally {
				pool.returnResource(jedis);
			}
			return res;
		}
/*
		public String existsChat(JSONArray participants, String appId) {
			Jedis jedis = pool.getResource();
			String strParticipants = Utils.getStringByJSONArray(participants, SEPARATOR3);
			try {
				Set<String> chats = jedis.keys(appId+":Chat_*");
				Iterator<String> it =  chats.iterator();
				while(it.hasNext()){
					String chatRoomId = it.next();
					String strParticipantsCurr = jedis.hget(chatRoomId, ChatMessage.PARTICIPANTS);
					if(strParticipantsCurr.equals(strParticipants)){
						String[] aux = chatRoomId.split(SEPARATOR1);
						return aux[1];
					}
				}
			}catch(Exception e){
				Log.error("", this, "getMessageList", "Error getMessageList redis.", e); 
			} finally {
				pool.returnResource(jedis);
			}
			return null;
		}
*/
		public String existsChat(String participants, String appId) {
			Jedis jedis = pool.getResource();
			String res=null;
			try {
				 res = jedis.get(appId+SEPARATOR1+participants);
			}catch(Exception e){
				Log.error("", this, "getMessageList", "Error getMessageList redis.", e); 
			} finally {
				pool.returnResource(jedis);
			}
			return res;
		}
		
		public int readMessages(String appId, String userId, JSONArray jsonArray) {
			int res = 0;
			Long aux = (long) 0;
			if(jsonArray.length()>0){
				try {
					for(int i = 0;i<jsonArray.length();i++){
						aux = jedis.lrem(appId+SEPARATOR2+"UnRead"+SEPARATOR2+userId,0, jsonArray.getString(i));
						res += ((int)(long)aux);
					}					
				} catch (JSONException e) {
					Log.error("", this, "readMessages", "Error readMessages redis.", e); 
				}		
			}
			return res;
		}

		public Boolean existsKey(String appId, String key) {
			Boolean res = false;
			try {
				res = jedis.exists(appId+SEPARATOR1+key);		
			} catch (Exception e) {
				Log.error("", this, "existsKey", "Error existsKey redis.", e); 
			}	
			return res;
		}

		//max 99999999 elements
		public List<String> getTotalUnreadMsg(String appId, String userId) {
			List<String> res = new ArrayList<String>();
			try {
				res = jedis.lrange(appId+SEPARATOR2+"UnRead"+SEPARATOR2+userId, 0, MAXELEMS);		
			} catch (Exception e) {
				Log.error("", this, "getTotalListElements", "Error getTotalListElements redis.", e); 
			}	
			return res;
		}

		public Boolean hasNotification(String appId, String roomId) {
			Boolean res = false;
			Jedis jedis = pool.getResource();
			try {
				res = (Boolean.parseBoolean(jedis.hget(appId+SEPARATOR1+roomId, ChatRoom.FLAG_NOTIFICATION)));
			} finally {
				pool.returnResource(jedis);
			}
			return res;
		}

		

		
}
