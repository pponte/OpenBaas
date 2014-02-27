package infosistema.openbaas.dataaccess.models;

import infosistema.openbaas.data.models.Application;
import infosistema.openbaas.data.models.Certificate;
import infosistema.openbaas.utils.Const;

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
			jedis.hset(appId+SEPARATOR1+cert.getClientId(), Application.APNS_CLIENT_ID, cert.getClientId());
			jedis.hset(appId+SEPARATOR1+cert.getClientId(), Application.APNS_CERTIFICATION_PATH, cert.getCertificatePath());
			jedis.hset(appId+SEPARATOR1+cert.getClientId(), Application.APNS_PASSWORD, cert.getAPNSPassword());
			jedis.hset(appId+SEPARATOR1+cert.getClientId(), Application.CREATION_DATE, String.valueOf(milliseconds));
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
			String path = jedis.hget(appId+SEPARATOR1+clientId, Application.APNS_CERTIFICATION_PATH);
			String pass = jedis.hget(appId+SEPARATOR1+clientId, Application.APNS_PASSWORD);
			long l = Long.valueOf(jedis.hget(appId+SEPARATOR1+clientId, Application.CREATION_DATE));
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
			jedis.hset(appId+SEPARATOR1+DEVICE+SEPARATOR1+userId+SEPARATOR1+clientId+SEPARATOR1+device.getDeviceId(), DEVICEID, device.getDeviceId());
			jedis.hset(appId+SEPARATOR1+DEVICE+SEPARATOR1+userId+SEPARATOR1+clientId+SEPARATOR1+device.getDeviceId(), DEVICETOKEN, device.getToken());
			jedis.hset(appId+SEPARATOR1+DEVICE+SEPARATOR1+userId+SEPARATOR1+clientId+SEPARATOR1+device.getDeviceId(), LASTREGISTER, milliseconds.toString());
			res = true;
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}
	
	public Device getDevice(String appId,String userId,String clientId, String deviceId) {
		Device res = new BasicDevice();
		Jedis jedis = pool.getResource();
		try {
			String deviceToken = jedis.hget(appId+SEPARATOR1+DEVICE+SEPARATOR1+userId+SEPARATOR1+clientId+SEPARATOR1+deviceId, DEVICETOKEN);
			long l = Long.valueOf(jedis.hget(appId+SEPARATOR1+DEVICE+SEPARATOR1+userId+SEPARATOR1+clientId+SEPARATOR1+deviceId, LASTREGISTER));
			if(deviceToken!=null) res.setDeviceId(deviceToken);
			res.setLastRegister(new Timestamp(l));
			res.setDeviceId(deviceId);
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}
	
	public Boolean removeDevice(String appId,String userId,String clientId, String deviceId) {
		Boolean res = false;
		Jedis jedis = pool.getResource();
		try {
			if(jedis.exists(appId+SEPARATOR1+DEVICE+SEPARATOR1+userId+SEPARATOR1+clientId+SEPARATOR1+deviceId)){
				jedis.del(appId+SEPARATOR1+DEVICE+SEPARATOR1+userId+SEPARATOR1+clientId+SEPARATOR1+deviceId);
				res = true;
			}
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}
	
	public Boolean addDeviceId(String appId,String userId, String clientId, String deviceId) {
		Boolean res = false;
		Jedis jedis = pool.getResource();
		try {
			jedis.rpush(appId+SEPARATOR2+DTLIST+SEPARATOR2+userId+SEPARATOR2+clientId, deviceId);
			res = true;
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}
	
	public Boolean removeDeviceId(String appId,String userId, String clientId, String deviceId) {
		Boolean res = false;
		Jedis jedis = pool.getResource();
		try {
			Long aux = jedis.lrem(appId+SEPARATOR2+"DTLIST"+SEPARATOR2+userId+SEPARATOR2+clientId,0, deviceId);
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
			aux = jedis.lrange(appId+SEPARATOR2+"DTLIST"+SEPARATOR2+userId+SEPARATOR2+clientId, 0, MAXELEMS);
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
