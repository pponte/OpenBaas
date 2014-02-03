package infosistema.openbaas.data.models;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UsersState {

	String userId;
	Boolean online;
	Date lastUpdateDate;
	
	public UsersState(){
		
	}

	public UsersState(String userId, Boolean online, Date lastUpdateDate) {
		super();
		this.userId = userId;
		this.online = online;
		this.lastUpdateDate = lastUpdateDate;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Boolean getOnline() {
		return online;
	}

	public void setOnline(Boolean online) {
		this.online = online;
	}

	public Date getLastUpdateDate() {
		return lastUpdateDate;
	}

	public void setLastUpdateDate(Date lastUpdateDate) {
		this.lastUpdateDate = lastUpdateDate;
	}
	
	
	
}
