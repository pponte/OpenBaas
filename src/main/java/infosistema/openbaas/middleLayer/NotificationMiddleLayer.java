package infosistema.openbaas.middleLayer;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javapns.devices.Device;
import javapns.devices.implementations.basic.BasicDevice;
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
		Device device = new BasicDevice();
		String devId = Utils.getRandomString(Const.getIdLength());
		Timestamp time = new Timestamp(new Date().getTime());
		device.setDeviceId(devId);
		device.setLastRegister(time);
		device.setToken(deviceToken);
		Boolean addId = noteModel.addDeviceId(appId, userId, clientId, devId);
		Boolean addDev = noteModel.createUpdateDevice(appId, userId, clientId, device);
		if(addId && addDev)
			res.put(clientId, device);
		else 
			res = null;
		return res;
	}
	
	
	//TODO JM
	
	
	
	
	
}
