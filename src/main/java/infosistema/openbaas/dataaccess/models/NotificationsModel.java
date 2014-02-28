package infosistema.openbaas.dataaccess.models;

import infosistema.openbaas.data.models.Application;
import infosistema.openbaas.data.models.Certificate;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javapns.devices.Device;
import javapns.devices.implementations.basic.BasicDevice;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class NotificationsModel {

	private static final String SEPARATOR1 = ":";
	private static final String SEPARATOR2 = "_";
	private static final String DTLIST = "DTList";
	private static final String CERT = "Cert";
	private static final String DEVICE = "Device";
	private static final int MAXELEMS = 9999999;
	private static final String DEVICEID = "deviceId";
	public static final String DEVICETOKEN = "deviceToken";
	public static final String CLIENTID = "clientId";
	private static final String LASTREGISTER= "lastRegister";
	
	private JedisPool pool;
	
	public NotificationsModel() {
		pool = new JedisPool(new JedisPoolConfig(), Const.getRedisChatServer(),Const.getRedisChatPort());
	}

	public Boolean createUpdateCertificate(String appId, Certificate cert) {
		Boolean res = false;
		Jedis jedis = pool.getResource();
		long milliseconds = cert.getCreatedDate().getTime();
		try {
			jedis.hset(appId+SEPARATOR1+CERT+SEPARATOR1+cert.getClientId(), Application.APNS_CLIENT_ID, cert.getClientId());
			jedis.hset(appId+SEPARATOR1+CERT+SEPARATOR1+cert.getClientId(), Application.APNS_CERTIFICATION_PATH, cert.getCertificatePath());
			jedis.hset(appId+SEPARATOR1+CERT+SEPARATOR1+cert.getClientId(), Application.APNS_PASSWORD, cert.getAPNSPassword());
			jedis.hset(appId+SEPARATOR1+CERT+SEPARATOR1+cert.getClientId(), Application.CREATION_DATE, String.valueOf(milliseconds));
			res = true;
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	public Certificate getCertificate(String appId, String clientId) {
		Certificate res = null;
		Jedis jedis = pool.getResource();
		try {
			String path = jedis.hget(appId+SEPARATOR1+CERT+SEPARATOR1+clientId, Application.APNS_CERTIFICATION_PATH);
			String pass = jedis.hget(appId+SEPARATOR1+CERT+SEPARATOR1+clientId, Application.APNS_PASSWORD);
			long l = Long.valueOf(jedis.hget(appId+SEPARATOR1+CERT+SEPARATOR1+clientId, Application.CREATION_DATE));
			res = new Certificate();
			res.setClientId(clientId);
			res.setCertificatePath(path);
			res.setAPNSPassword(pass);
			res.setCreatedDate(new Timestamp(l));
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}
		
	public Boolean createUpdateDevice(String appId,String userId,String clientId, Device device) {
		Boolean res = false;
		Jedis jedis = pool.getResource();
		Long milliseconds = device.getLastRegister().getTime();
		try {
			jedis.hset(appId+SEPARATOR1+DEVICE+SEPARATOR1+clientId+SEPARATOR1+device.getToken(), DEVICEID, device.getDeviceId());
			jedis.hset(appId+SEPARATOR1+DEVICE+SEPARATOR1+clientId+SEPARATOR1+device.getToken(), DEVICETOKEN, device.getToken());
			jedis.hset(appId+SEPARATOR1+DEVICE+SEPARATOR1+clientId+SEPARATOR1+device.getToken(), LASTREGISTER, milliseconds.toString());
			jedis.hset(appId+SEPARATOR1+DEVICE+SEPARATOR1+clientId+SEPARATOR1+device.getToken(),"userId", userId);
			res = true;
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}
	
	public Device getDevice(String appId,String userId,String clientId, String deviceToken) {
		Device res = new BasicDevice();
		Jedis jedis = pool.getResource();
		try {
			Log.info("", "", "hget", "hget->" +"key:" +appId+SEPARATOR2+DTLIST+SEPARATOR2+userId+SEPARATOR2+clientId+" - min:"+"0"+" - max:"+MAXELEMS);
			String deviceId = jedis.hget(appId+SEPARATOR1+DEVICE+SEPARATOR1+clientId+SEPARATOR1+deviceToken, DEVICEID);
			long l = Long.valueOf(jedis.hget(appId+SEPARATOR1+DEVICE+SEPARATOR1+clientId+SEPARATOR1+deviceToken, LASTREGISTER));
			res.setLastRegister(new Timestamp(l));
			res.setToken(deviceToken);
			res.setDeviceId(deviceId);
			//res.setUserId(userId);
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}
	
	public Boolean removeDevice(String appId,String userId,String clientId, String deviceToken) {
		Boolean res = false;
		Jedis jedis = pool.getResource();
		try {
			if(jedis.exists(appId+SEPARATOR1+DEVICE+SEPARATOR1+userId+SEPARATOR1+clientId+SEPARATOR1+deviceToken)){
				jedis.del(appId+SEPARATOR1+DEVICE+SEPARATOR1+userId+SEPARATOR1+clientId+SEPARATOR1+deviceToken);
				res = true;
			}
			else
				res = true;
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}
	
	public Boolean addDeviceId(String appId,String userId, String clientId, String deviceToken) {
		Boolean res = false;
		Jedis jedis = pool.getResource();
		try {
			jedis.rpush(appId+SEPARATOR2+DTLIST+SEPARATOR2+userId+SEPARATOR2+clientId, deviceToken);
			res = true;
		} catch(Exception e){
			res = false;
			Log.error("", this, "addDeviceId", "Error in addDeviceId to list", e);
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}
	
	public Boolean removeDeviceId(String appId,String userId, String clientId, String deviceToken) {
		Boolean res = false;
		Jedis jedis = pool.getResource();
		try {
			Long aux = jedis.lrem(appId+SEPARATOR2+DTLIST+SEPARATOR2+userId+SEPARATOR2+clientId,0, deviceToken);
			if(aux>0)				
				res = true;
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}
	
	public List<Device> getDeviceIdList(String appId,String userId, String clientId) {
		List<Device> res = new ArrayList<Device>();
		List<String> aux = new ArrayList<String>();
		Jedis jedis = pool.getResource();
		try {
			Log.info("", "", "push", "lrange ->" +"key:" +appId+SEPARATOR2+DTLIST+SEPARATOR2+userId+SEPARATOR2+clientId+" - min:"+"0"+" - max:"+MAXELEMS);
			aux = jedis.lrange(appId+SEPARATOR2+DTLIST+SEPARATOR2+userId+SEPARATOR2+clientId, 0, MAXELEMS);
			Iterator<String> it = aux.iterator();
			while(it.hasNext()){
				res.add(getDevice(appId, userId, clientId, it.next()));
			}
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	



	
}
