package infosistema.openbaas.middleLayer;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javapns.devices.Device;
import infosistema.openbaas.data.models.DeviceOB;
import infosistema.openbaas.dataaccess.models.NotificationsModel;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Utils;


public class NotificationMiddleLayer {

	private NotificationsModel noteModel;


	// *** INSTANCE *** //
	private static NotificationMiddleLayer instance = null;
	
	private NotificationMiddleLayer() {
		super();
		noteModel = new NotificationsModel();
	}
	
	public static NotificationMiddleLayer getInstance() {
		if (instance == null) instance = new NotificationMiddleLayer();
		return instance;
	}

	public Map<String, Device> addDeviceToken(String appId, String userId, String clientId, String deviceToken) {
		Map<String, Device> res = new HashMap<String, Device>();
		Device device = new DeviceOB();
		String devId = Utils.getRandomString(Const.getIdLength());
		Timestamp time = new Timestamp(new Date().getTime());
		device.setDeviceId(devId);
		device.setLastRegister(time);
		device.setToken(deviceToken);
		Boolean addDev = noteModel.createUpdateDevice(appId, userId, clientId, device);
		Boolean addId = noteModel.addDeviceId(appId, userId, clientId, deviceToken); //TODO mete id repetidos na lista
		if(addId && addDev)
			res.put(clientId, device);
		else 
			res = null;
		return res;
	}
	
	public Boolean remDeviceToken(String appId, String userId, String clientId, String deviceToken) {
		Boolean res = false;
		
		Boolean remDev = noteModel.removeDevice(appId, userId, clientId, deviceToken);
		Boolean remId = noteModel.removeDeviceId(appId, userId, clientId, deviceToken);
		if(remDev && remId)
			res = true;
		return res;
	}
	
	
	//TODO JM
	
	
	
	
	
}
