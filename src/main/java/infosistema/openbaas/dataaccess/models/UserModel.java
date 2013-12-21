package infosistema.openbaas.dataaccess.models;

import infosistema.openbaas.data.enums.OperatorEnum;
import infosistema.openbaas.data.models.User;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class UserModel {

	// request types
	private JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisGeneralServer(),Const.getRedisGeneralPort());
	Jedis jedis;
	
	public UserModel() {
		jedis = new Jedis(Const.getRedisGeneralServer(), Const.getRedisGeneralPort());
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	// *** *** USERS *** *** //
	
	// *** CREATE *** //

	public Boolean createUser(String appId, String userId, String userName, String socialId, String socialNetwork,
			String email, byte[] salt, byte[] hash, String flag, Boolean emailConfirmed, Boolean baseLocationOption, String baseLocation, String location) throws UnsupportedEncodingException {
		Jedis jedis = pool.getResource();
		Boolean res = false;
		try {
			if (!jedis.exists("users:" + userId)) {
				long unixTime = System.currentTimeMillis() / 1000L;
				jedis.zadd("users:time", unixTime, appId + ":" + userId);
				jedis.hset("users:" + userId, "userId", userId);
				if(userName!=null)
					jedis.hset("users:" + userId, "userName", userName);
				jedis.hset("users:" + userId, socialNetwork+"_id", socialId);
				jedis.hset("users:" + userId, "email", email);
				jedis.hset("users:" + userId, "salt", new String(salt, "ISO-8859-1"));
				jedis.hset(("users:" + userId), "hash", new String(hash, "ISO-8859-1"));
				jedis.hset("users:" + userId, "alive", new String("true"));				
				jedis.hset("users:" + userId, "baseLocationOption", baseLocationOption.toString());
				if(baseLocation!=null)
					jedis.hset("users:" + userId, "baseLocation", baseLocation.toString());
				else
					jedis.hset("users:" + userId, "baseLocation", "null");
				if(location!=null)
					jedis.hset("users:" + userId, Const.LOCATION, location);
				jedis.sadd("app:" + appId + ":users", userId);
				jedis.sadd("app:" + appId + ":users:emails", email);
				if(flag!=null)
					jedis.hset(("users:" + userId), "userFile", flag);
				if (emailConfirmed != null) 
					jedis.hset("users:"+userId, "emailConfirmed", "" + emailConfirmed);
				res = true;
			}
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}


	// *** UPDATE *** //

	/**
	 * Updates the user, depending on the fields. If the only field sent by the
	 * request was alive, then only the alive field is updated.
	 * 
	 * @param appId
	 * @param userId
	 * @param email
	 * @param hash
	 * @param salt
	 * @param alive
	 * @throws UnsupportedEncodingException 
	 */
	
	public Boolean updateUser(String appId, String userId, String userName,	String email, 
			String userFile, Boolean baseLocationOption, String baseLocation, String location) {
		Jedis jedis = pool.getResource();
		Boolean res = false;
		try {
			jedis.hset("users:" + userId, "userName", userName);
			jedis.hset("users:" + userId, "email", email);
			jedis.hset("users:" + userId, "alive", new String("true"));				
			jedis.hset("users:" + userId, "baseLocationOption", baseLocationOption.toString());
			if(baseLocation!=null)
				jedis.hset("users:" + userId, "baseLocation", baseLocation.toString());
			else
				jedis.hset("users:" + userId, "baseLocation", "null");
			if(location!=null)
				jedis.hset("users:" + userId, Const.LOCATION, location);
			if(userFile!=null)
				jedis.hset(("users:" + userId), "userFile", userFile);
			res = true;
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}
	
	public Boolean updateUserEmail(String appId, String oldEmail, String newEmail) {
		Jedis jedis = pool.getResource();
		Boolean res = false;
		try{
			//"app:68c:users:emails"
			jedis.srem("app:"+appId+":users:emails", oldEmail);
			jedis.sadd("app:"+appId+":users:emails", newEmail);
			res = true;
		}catch(Exception e){
			Log.error("", this, "updateUserEmail", "updateUserEmail", e);
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}


	public void updateUserRecover(String appId, String userId, String email,
			byte[] hash, byte[] salt) throws UnsupportedEncodingException {
		Jedis jedis = pool.getResource();
		try {
			jedis.hset("users:" + userId, "email", email);
			jedis.hset(("users:" + userId), "salt", new String(salt,"ISO-8859-1"));
			jedis.hset(("users:" + userId), "hash", new String(hash,"ISO-8859-1"));
			long unixTime = System.currentTimeMillis() / 1000L;
			jedis.zadd("users:time", unixTime, appId + ":" + userId);

		} finally {
			pool.returnResource(jedis);
		}
	}

	public void updateUserLocation(String userId, String appId, String location) {
		Jedis jedis = pool.getResource();
		try{
			jedis.hset(("users:" + userId), Const.LOCATION, location);
		}finally{
			pool.returnResource(jedis);
		}
	}

	public Boolean updateUserPassword(String appId, String userId, byte[] hash,
			byte[] salt) throws UnsupportedEncodingException {
		Jedis jedis = pool.getResource();
		try {
			jedis.hset(("users:" + userId), "hash", new String(hash, "ISO-8859-1"));
			jedis.hset("users:" + userId, "salt", new String(salt, "ISO-8859-1"));
			long unixTime = System.currentTimeMillis() / 1000L;
			jedis.zadd("users:time", unixTime, appId + ":" + userId);
		} finally {
			pool.returnResource(jedis);
		}
		return true;
	}

	// *** GET LIST *** //
	
	public ArrayList<String> getAllUserIdsForApp(String appId,
			Integer pageNumber, Integer pageSize, String orderBy,
			String orderType) {
		// TODO Auto-generated method stub
		return null;
	}


	// *** GET LIST *** //

	
	
	public List<String> getOperation(String appId, OperatorEnum oper, String attribute, String value) throws Exception {
		Jedis jedis = pool.getResource();
		List<String> listRes = new ArrayList<String>();
		try{
			Set<String> setUsers = jedis.smembers("app:"+appId+":users:emails");
			Iterator<String> iter = setUsers.iterator();
			while(iter.hasNext()){
				String userId = iter.next();
				Map<String, String> mapUser = getUser(appId, userId);
				if(mapUser.containsKey(attribute)){
					if(mapUser.get(attribute).equals(value))
						listRes.add(userId);
				}
			}
		}
		catch (Exception e) {
			throw e;
		}
		return listRes;
	}

	public Set<String> getAllUserIdsForApp(String appId) {
		Jedis jedis = pool.getResource();
		Set<String> res = null;
		try {
			 res = jedis.smembers("app:"+appId+":users");	
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}


	// *** GET *** //

	/**
	 * Checks if user is present in the app:{appId}:users and if it is returns
	 * its fields
	 * 
	 * @param appId
	 * @param userId
	 * @return
	 */
	public Map<String, String> getUser(String appId, String userId) {
		Jedis jedis = pool.getResource();
		Map<String, String> userFields = null;
		try {
			Set<String> usersOfApp = jedis.smembers("app:" + appId + ":users");
			Iterator<String> it = usersOfApp.iterator();
			Boolean userExistsforApp = false;
			while (it.hasNext())
				if (it.next().equalsIgnoreCase(userId))
					userExistsforApp = true;
			if (userExistsforApp) {
				userFields = jedis.hgetAll("users:" + userId);
			}
			if(userFields.get("baseLocation").equals("null"))
				userFields.put("baseLocation", null);
		} finally {
			pool.returnResource(jedis);
		}
		return userFields;
	}

	public Boolean identifierInUseByUserInApp(String appId, String userId) {
		Jedis jedis = pool.getResource();
		Boolean userExists = false;
		try {
			Set<String> usersInApp = jedis.smembers("app:" + appId	+ ":users");
			Iterator<String> it = usersInApp.iterator();
			while (it.hasNext())
				if (it.next().equalsIgnoreCase(userId))
					userExists = true;
		} finally {
			pool.returnResource(jedis);
		}
		return userExists;
	}

	public String getUserNameUsingUserId(String appId, String userId) {
		Jedis jedis = pool.getResource();
		String userName = null;
		try {
			Set<String> usersInApp = jedis.smembers("app:" + appId
					+ ":users");
			Iterator<String> it = usersInApp.iterator();
			while (it.hasNext())
				if (it.next().equalsIgnoreCase(userId))
					userName = jedis.hget("users:" + userId, "userName");
		} finally {
			pool.returnResource(jedis);
		}
		return userName;
	}

	public String getEmailUsingUserId(String appId, String userId) {
		Jedis jedis = pool.getResource();
		String email = null;
		try {
			Set<String> usersInApp = jedis.smembers("app:" + appId
					+ ":users");
			Iterator<String> it = usersInApp.iterator();
			while (it.hasNext())
				if (it.next().equalsIgnoreCase(userId))
					email = jedis.hget("users:" + userId, "email");
		} finally {
			pool.returnResource(jedis);
		}
		return email;
	}
	
	public String getUserIdUsingSocialInfo(String appId, String socialId, String socialNetwork) {
		Jedis jedis = pool.getResource();
		String userId = null;
		try {
			Set<String> usersInApp = jedis.smembers("app:" + appId	+ ":users");
			Iterator<String> it = usersInApp.iterator();
			while (it.hasNext()){
				String userAux = it.next();
				String socialTemp = jedis.hget("users:" + userAux, socialNetwork+"_id");
				if (socialTemp.equals(socialId))
					userId = userAux;
			}
		} finally {
			pool.returnResource(jedis);
		}
		return userId;
	}	

	public String getEmailUsingUserName(String appId, String userName) {
		Jedis jedis = pool.getResource();
		try {
			Set<String> usersInApp = jedis.smembers("app:" + appId + ":users");
			Iterator<String> it = usersInApp.iterator();
			while (it.hasNext()) {
				String userId = it.next();
				Map<String, String> userFields = jedis.hgetAll("users:"
						+ userId);
				if (userFields.get("userName").equalsIgnoreCase(userName))
					return userFields.get("email");
			}

		} finally {
			pool.returnResource(jedis);
		}
		return null;
	}

	public String getUserIdUsingUserName(String appId, String userName) {
		Jedis jedis = pool.getResource();
		try {
			Set<String> usersInApp = jedis.smembers("app:" + appId + ":users");
			Iterator<String> it = usersInApp.iterator();
			while (it.hasNext()) {
				String userId = it.next();
				Map<String, String> userFields = jedis.hgetAll("users:"
						+ userId);
				if (userFields.get("userName").equalsIgnoreCase(userName))
					return userFields.get("userId");
			}
		} finally {
			pool.returnResource(jedis);
		}
		return null;
	}
	
	public String getUserIdUsingEmail(String appId, String email) {
		Jedis jedis = pool.getResource();
		try {
			Set<String> usersInApp = jedis.smembers("app:" + appId + ":users");
			Iterator<String> it = usersInApp.iterator();
			while (it.hasNext()) {
				String userId = it.next();
				Map<String, String> userFields = jedis.hgetAll("users:"	+ userId);
				if (userFields.get("email").equalsIgnoreCase(email))
					return userFields.get("userId");
			} 
		} finally {
			pool.returnResource(jedis);
		}
		return null;
	}

	public User getUserUsingEmail(String appId, String email) {
		Jedis jedis = pool.getResource();
		User res = new User();
		try {
			Set<String> usersInApp = jedis.smembers("app:" + appId + ":users");
			Iterator<String> it = usersInApp.iterator();
			while (it.hasNext()) {
				String userId = it.next();
				Map<String, String> userFields = jedis.hgetAll("users:"	+ userId);
				if (userFields.get("email").equalsIgnoreCase(email)){
					res.setUserID(userFields.get("userId"));
					res.setUserName(userFields.get("userName"));
					res.setUserFile(userFields.get("userFile"));
					if(userFields.get("baseLocation").equals("null"))
						res.setBaseLocation(null);
					else
						res.setBaseLocation(userFields.get("baseLocation"));
					res.setLastLocation(userFields.get("location"));
					res.setBaseLocationOption(Boolean.parseBoolean(userFields.get("baseLocationOption")));
				}
			} 
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	
	// *** DELETE *** //

	/**
	 * If forever is true, then it deletes the user forever, if not it sets it
	 * as inactive.
	 * 
	 * @param userId
	 * @return
	 */
	public Boolean deleteUser(String appId, String userId) {
		Jedis jedis = pool.getResource();
		Boolean sucess = false;
		try {
			if (jedis.exists("users:"+userId)) {
				jedis.zrem("users:time", userId);
				Client c1 = jedis.getClient();
				c1.getPort();
				jedis.hset("users:" + userId, "alive", "false");
				jedis.sadd("app:" + appId + ":users:inactive", appId + ":" + userId);
				sucess = true;
			} else {
				sucess = false;
			}
		} finally {
			pool.returnResource(jedis);
		}
		return sucess;
	}


	// *** EXISTS *** //

	public Boolean userExistsInApp(String appId, String email) {
		Jedis jedis = pool.getResource();
		Boolean userExists = false;
		try {
			//Set<String> usersInApp = jedis.smembers("app:" + appId + ":users");
			//Iterator<String> it = usersInApp.iterator();
			if (jedis.sismember("app:" + appId + ":users:emails", email))
				userExists = true;
		} finally {
			pool.returnResource(jedis);
		}
		return userExists;
	}
	
	public Boolean socialUserExistsInApp(String appId, String socialId,	String socialNetwork) {
		Jedis jedis = pool.getResource();
		Boolean userExists = false;
		try {
			//Set<String> usersInApp = jedis.smembers("app:" + appId + ":users");
			//Iterator<String> it = usersInApp.iterator();
			if (jedis.sismember("app:" + appId + ":users:"+socialNetwork+"_id", socialId))
				userExists = true;
		} finally {
			pool.returnResource(jedis);
		}
		return userExists;
	}


	// *** OTHERS *** //

	public Boolean confirmUserEmail(String appId, String userId) {
		Jedis jedis = pool.getResource();
		try {
			jedis.hset("users:"+userId, "emailConfirmed", true+"");
		}finally {
			pool.returnResource(jedis);
		}
		return true;
	}

	public Boolean userEmailIsConfirmed(String appId, String userId) {
		Jedis jedis = pool.getResource();
		Boolean isConfirmed = false;
		try {
			isConfirmed = Boolean.parseBoolean(jedis.hget("users:"+userId, "emailConfirmed"));
		}finally {
			pool.returnResource(jedis);
		}
		return isConfirmed;
	}

	
	

}
