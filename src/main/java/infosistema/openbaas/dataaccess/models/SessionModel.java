package infosistema.openbaas.dataaccess.models;

import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.geolocation.Geo;

import java.io.UnsupportedEncodingException;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Sessions are stored in a separate redis instance, this redis instance is used
 * only for session operations.
 * 
 * Redis Sessions Instance -> Sessions Redis Cache Instance -> Everything else
 * (users/apps/ect)
 * 
 */
public class SessionModel {

	// *** CONTRUCTORS *** //

	public SessionModel() {
		userModel  = new UserModel();
		geo = Geo.getInstance();
	}
	

	// *** PRIVATE *** //
	
	private Geo geo;
	private UserModel userModel;

	
	// *** CONSTANTS *** //

	// *** KEYS *** //

	// *** CREATE *** //
	
	public void createAdmin(String OPENBAASADMIN, byte[] adminSalt, byte[] adminHash) throws UnsupportedEncodingException {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
		Jedis jedis = pool.getResource();
		try {
			jedis.hset(OPENBAASADMIN, "adminSalt", new String(adminSalt,"ISO-8859-1"));
			jedis.hset(OPENBAASADMIN, "adminHash", new String(adminHash,"ISO-8859-1"));
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
	}

	/**
	 * For sessions we use Redis expire mechanism, keys with more than 24 hours
	 * are automatically removed.
	 * 
	 * @param sessionId
	 * @param userId
	 */
	public void createSession(String sessionId, String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(),	Const.getRedisSessionPort());
		Jedis jedis = pool.getResource();
		try {
			jedis.sadd("sessions:set", sessionId);
			jedis.hset("sessions:" + sessionId, Const.USER_ID, userId);
			jedis.expire("sessions:" + sessionId, Const.getSessionExpireTime());
			jedis.sadd("user:sessions:" + userId, sessionId);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
	}


	// *** UPDATE *** //
	
	/**
	 * Updates the session time, if it had 10 hours left until being deleted
	 * after we call refreshSession it will have EXPIRETIME until it is deleted
	 * (by default 24 hours).
	 */
	public boolean refreshSession(String sessionToken, String location, String date, String userAgent) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
		Jedis jedis = pool.getResource();
		try {
			jedis.expire("sessions:" + sessionToken, Const.getSessionExpireTime());
			jedis.hset("sessions:" + sessionToken, "date", date);
			
			boolean locationHasChange = false;
			if (location != null && !"".equals(location)) {
				String previousLocation = jedis.hget("sessions:" + sessionToken, Const.LOCATION);
				if (previousLocation == null){
					locationHasChange = true;
					previousLocation = "0:0";
				}

				String[] previousLocationArray = previousLocation.split(":");
				String[] currentLocationArray = location.split(":");
				double previousLatitudeValue, previousLongitudeValue, currentLatitudeValue, currentLongitudeValue;
				try{
					previousLatitudeValue = Double.parseDouble(previousLocationArray[0]);
					previousLongitudeValue = Double.parseDouble(previousLocationArray[1]);
	
					currentLatitudeValue = Double.parseDouble(currentLocationArray[0]);
					currentLongitudeValue = Double.parseDouble(currentLocationArray[1]);
				}catch (NumberFormatException e){
					Log.error("", this, "refreshSession", "Wrong number format of " +
							"previousLocationArray[0]=" + previousLocationArray[0] + " or " +
							"previousLocationArray[1=]=" + previousLocationArray[1] + " or " +
							"currentLocationArray[0]=" + currentLocationArray[0] + " or " +
							"currentLocationArray[1]=" + currentLocationArray[1], e); 
					return false;
				}
				// Test if distance < MAXIMUM DISTANCE Spherical Law of Cosines
				double dist = geo.distance(previousLatitudeValue, previousLongitudeValue, currentLatitudeValue, currentLongitudeValue);
				locationHasChange = (dist >= 1);
			}
			if (locationHasChange) {
				String userId = this.getUserIdUsingSessionToken(sessionToken);
				String appId = this.getAppUsingSessionToken(sessionToken);
				updateLocationToSession(appId, userId, sessionToken, location);
				jedis.hset("sessions:" + sessionToken, Const.LOCATION, location);
			}
		}catch(Exception e){
			Log.error("", "refreshSession", "refreshSession", e.toString());
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return true;
	}
	
	private void updateLocationToSession(String appId, String userId, String sessionToken, String location) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
		Jedis jedis = pool.getResource();
		try {
			jedis.hset("sessions:" + sessionToken, Const.LOCATION, location);
			userModel.updateUserLocation(appId, userId, location);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
	}

	
	// *** DELETE *** //
	
	// *** GET LIST *** //
	
	// *** GET *** //
	
	public Map<String, String> getAdminFields(String OPENBAASADMIN) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(),	Const.getRedisSessionPort());
		Jedis jedis = pool.getResource();
		Map<String, String> adminFields = null;
		try {
			adminFields = jedis.hgetAll(OPENBAASADMIN);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return adminFields;
	}

	
	// *** OTHERS *** //

	public boolean adminExists(String admin) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
		Jedis jedis = pool.getResource();
		boolean adminExists = false;
		try {
			if (jedis.exists(admin))
				adminExists = true;
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return adminExists;
	}

	public void createSession(String sessionId, String appId, String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
		Jedis jedis = pool.getResource();
		try {
			jedis.sadd("sessions:set", sessionId);
			jedis.hset("sessions:" + sessionId, Const.APP_ID, appId);
			jedis.hset("sessions:" + sessionId, Const.USER_ID, userId);
			jedis.expire("sessions:" + sessionId, Const.getSessionExpireTime());
			jedis.sadd("user:sessions:" + userId, sessionId);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
	}

	public Map<String, String> getSessionFields(String sessionId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
		Jedis jedis = pool.getResource();
		Map<String, String> sessionFields = null;
		try {
			sessionFields = jedis.hgetAll("sessions:" + sessionId);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return sessionFields;
	}

	public boolean sessionTokenExists(String sessionId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
		Jedis jedis = pool.getResource();
		boolean sucess = false;
		try {
			Set<String> sessionIds = jedis.smembers("sessions:set");
			Iterator<String> it = sessionIds.iterator();
			while (it.hasNext()) {
				if (it.next().equalsIgnoreCase(sessionId) && jedis.exists("sessions:" + sessionId)){
					sucess = true;
					break;
				}
			}

		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return sucess;
	}

	public boolean sessionTokenExistsForUser(String sessionToken, String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
		Jedis jedis = pool.getResource();
		boolean sucess = false;
		try {
			Set<String> sessionIds = jedis.smembers("user:sessions:" + userId);
			Iterator<String> it = sessionIds.iterator();
			while (it.hasNext()) {
				String sessionId = it.next();
				if (jedis.exists("sessions:" + sessionId) && sessionId.equalsIgnoreCase(sessionToken))
					sucess = true;
				else
					jedis.srem("user:sessions:" + userId, sessionId);
			}
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return sucess;
	}

	public void deleteAdminSession(String adminId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
		Jedis jedis = pool.getResource();
		try {
			Set<String> sessionIds = jedis.smembers("sessions");
			Iterator<String> it = sessionIds.iterator();
			while (it.hasNext()) {
				String id = it.next();
				Map<String, String> sessionFields = jedis.hgetAll("sessions:" + id);
				String stmp = null;
				try {
					stmp = sessionFields.get(Const.USER_ID);
					if (stmp.equalsIgnoreCase(adminId)) {
						jedis.del("sessions:" + id);
						jedis.srem("sessions", adminId);
					}
				} catch (Exception e) { }
			}
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
	}

	public void createAdminSession(String sessionId, String adminId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
		Jedis jedis = pool.getResource();
		try {
			jedis.sadd("sessions", sessionId);
			jedis.hset("sessions:" + sessionId, "adminId", adminId);
			jedis.expire("sessions:" + sessionId, Const.getSessionExpireTime());
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
	}

	public boolean deleteUserSession(String sessionToken, String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
		Jedis jedis = pool.getResource();
		boolean sucess = false;
		try {
			Set<String> sessionIds = jedis.smembers("user:sessions:" + userId);
			Iterator<String> it = sessionIds.iterator();
			while (it.hasNext()) {
				String sessionId = it.next();
				if (sessionId.equalsIgnoreCase(sessionToken)) {
					jedis.srem("user:sessions:" + userId, sessionId);
					jedis.srem("sessions", sessionId);
					jedis.del("sessions:" + sessionId);
					sucess = true;
				}
			}
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return sucess;
	}

	public String getUserSession(String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
		Jedis jedis = pool.getResource();
		String sessionId = null;
		try {
			Set<String> sessionIds = jedis.smembers("sessions");
			Iterator<String> it = sessionIds.iterator();
			while (it.hasNext()) {
				String id = it.next();
				Map<String, String> sessionFields = jedis.hgetAll("sessions:" + id);
				try {
					String stmp = sessionFields.get(Const.USER_ID);
					if (stmp.equalsIgnoreCase(userId) && jedis.exists("sessions:" + id))
						sessionId = id;
				} catch (Exception e) { }
			}
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return sessionId;
	}

	public boolean deleteAllUserSessions(String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
		Jedis jedis = pool.getResource();
		boolean sucess = false;
		try {
			if (jedis.exists("user:sessions:" + userId)) {
				jedis.del("user:sessions:" + userId);
				sucess = true;
			}
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return sucess;
	}

	public Set<String> getAllUserSessions(String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
		Jedis jedis = pool.getResource();
		Set<String> userSessions = null;
		try {
			if (jedis.exists("user:sessions:" + userId)) {
				userSessions = jedis.smembers("user:sessions:" + userId);
			}
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return userSessions;
	}

	public boolean sessionExistsForUser(String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
		Jedis jedis = pool.getResource();
		boolean exists = false;
		try {
			Set<String> sessionIds = jedis.smembers("user:sessions:" + userId);
			Iterator<String> it = sessionIds.iterator();
			if (it.hasNext())
				exists = true;
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return exists;
	}
	
	public boolean isUserOnline(String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
		Jedis jedis = pool.getResource();
		boolean exists = false;
		try {
			Set<String> sessionIds = jedis.smembers("user:sessions:" + userId);
			Iterator<String> it = sessionIds.iterator();
			while (it.hasNext()){
				String sessionName = it.next();
				Long userTime = jedis.ttl("sessions:"+sessionName);
				if (userTime != -1)
					exists = true;
			}
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return exists;
	}
	
	public String getAppIdForSessionToken(String sessionToken) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
		Jedis jedis = pool.getResource();
		String retApp = null;
		try {
			retApp = jedis.hget("sessions:"+sessionToken, Const.APP_ID);
		}finally {
			pool.returnResource(jedis);
		}
		return retApp;
	}

	private String getAppUsingSessionToken(String sessionToken) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
		Jedis jedis = pool.getResource();
		String res = null;
		try {
			res = jedis.hget("sessions:" + sessionToken, Const.APP_ID);
		}finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	public String getUserIdUsingSessionToken(String sessionToken) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
		Jedis jedis = pool.getResource();
		String res = null;
		try {
			res = jedis.hget("sessions:" + sessionToken, Const.USER_ID);
		}finally {
			pool.returnResource(jedis);
		}
		return res;
	}
}
