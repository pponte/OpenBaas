package infosistema.openbaas.data.models;

import java.util.Date;

public class ChatMessage {

	private String _id;
	private Date date;
	private String sender;
	private String messageText;
	private String fileText;
	
	public final static String ORIENTATION = "orientation";
	public final static String MESSAGE_TEXT = "messageText";
	public final static String FILE_TEXT = "fileText";
	public final static String ROOM_ID = "roomId";
	public final static String PARTICIPANTS = "participants";
	public final static String DATE = "date";
	public final static String SENDER = "sender";
	public final static String _ID = "_id";
	public final static String NUM_MSG = "numberMessages";
	
	
	public ChatMessage(){
		
	}
	
	public ChatMessage(String _id, Date date, String sender,
			String messageText,String fileText) {
		super();
		this._id = _id;
		this.date = date;
		this.sender = sender;
		this.messageText = messageText;
		this.fileText = fileText;
	}
	
	public String get_id() {
		return _id;
	}
	
	public void set_id(String _id) {
		this._id = _id;
	}
	
	public Date getDate() {
		return date;
	}
	
	public void setDate(Date date) {
		this.date = date;
	}
	
	public String getSender() {
		return sender;
	}
	
	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getMessageText() {
		return messageText;
	}

	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}

	public String getFileText() {
		return fileText;
	}

	public void setFileText(String fileText) {
		this.fileText = fileText;
	}
	
	
}
