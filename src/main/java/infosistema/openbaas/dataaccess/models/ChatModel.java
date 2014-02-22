package infosistema.openbaas.dataaccess.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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
		

		public Boolean createMsg(String appId, ChatMessage msg) {
			Boolean res = false;
			Jedis jedis = pool.getResource();
			long milliseconds = msg.getDate().getTime();
			try {
				jedis.hset(appId+SEPARATOR1+msg.get_id(), ChatMessage._ID, msg.get_id());
				jedis.hset(appId+SEPARATOR1+msg.get_id(), ChatMessage.SENDER, msg.getSender());
				jedis.hset(appId+SEPARATOR1+msg.get_id(), ChatMessage.DATE, String.valueOf(milliseconds));
				try{jedis.hset(appId+SEPARATOR1+msg.get_id(), ChatMessage.MESSAGE_TEXT, msg.getMessageText());}catch(Exception e){}
				try{jedis.hset(appId+SEPARATOR1+msg.get_id(), ChatMessage.FILE_TEXT, msg.getFileText());}catch(Exception e){}
				res = true;
			} finally {
				pool.returnResource(jedis);
			}
			return res;
		}
		
		public Boolean addStartMsg2Room(String appId, String msgId,String roomId, String roomName, String roomCreator, Boolean flagNotification, String totalParticipants, List<String> unReadUsers) {
			Boolean res = false;
			Jedis jedis = pool.getResource();
			if(flagNotification==null) flagNotification = true;
			Iterator<String> it = unReadUsers.iterator();
			try {
				jedis.hset(appId+SEPARATOR1+roomId, ChatRoom._ID, roomId);
				jedis.hset(appId+SEPARATOR1+roomId, ChatRoom.ROOM_NAME, roomName);
				jedis.hset(appId+SEPARATOR1+roomId, ChatRoom.ROOM_CREATOR, roomCreator);
				jedis.hset(appId+SEPARATOR1+roomId, ChatRoom.FLAG_NOTIFICATION, flagNotification.toString());
				jedis.hset(appId+SEPARATOR1+roomId, ChatRoom.PARTICIPANTS, totalParticipants);
				jedis.rpush(appId+SEPARATOR2+roomId, msgId);
				while(it.hasNext()){
					jedis.rpush(appId+SEPARATOR2+it.next(), msgId);
				}
				res = true;
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
					jedis.rpush(appId+SEPARATOR2+it.next(), msgId);
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
				Integer startIndex = 0;
				int i = 0;
				int index = (int) (long)size;
				while(i<size){
					index = ((int)(long) (size))-i-1;
					i++;
					String msgIdCurr = jedis.lindex(appId+SEPARATOR2+roomId, index);
					long l = Long.valueOf(jedis.hget(appId+SEPARATOR1+msgIdCurr, ChatMessage.DATE)).longValue();
					Date dateCurr = new Date(l);
					if(dateCurr.before(date))
						break;
				}
				if(orientation.equals("front")){
					startIndex = index+1;
					if(index+numberMessages<endIndex)
						endIndex= index+numberMessages;
				}else{
					endIndex = index;
					startIndex = index-numberMessages+1;
					if(startIndex<0)
						startIndex=0;
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
					if(sender!=null) msg.setSender(sender);
					if(fileText!=null) msg.setFileText(fileText);
					if(messageText!=null) msg.setMessageText(messageText);
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
}
