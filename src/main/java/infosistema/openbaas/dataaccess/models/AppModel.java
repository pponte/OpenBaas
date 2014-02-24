package infosistema.openbaas.dataaccess.models;

import infosistema.openbaas.data.enums.FileMode;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.Application;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jettison.json.JSONObject;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class AppModel {

	// request types
	private JedisPool pool;// = new JedisPool(new JedisPoolConfig(), Const.getRedisGeneralServer(),Const.getRedisGeneralPort());
	//private Jedis jedis;
	private static final int MAXELEMS = 9999999;
	
	public AppModel() {
		pool = new JedisPool(new JedisPoolConfig(), Const.getRedisGeneralServer(),Const.getRedisGeneralPort());
		//jedis = pool.getResource();
		
		//jedis = new Jedis(Const.getRedisGeneralServer(), Const.getRedisGeneralPort());
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	// *** *** APPS *** *** //
	
	// *** PRIVATE *** //
	
	// *** CREATE *** //
	
	/**
	 * Return codes: 1 = Created application -1 = Application exists;
	 * 
	 * @param appId
	 * @param creationDate
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public Application createApp(String appId, String appKey, byte[] hash, byte[] salt, String appName, String creationDate, 
			Boolean confirmUsersEmail, Boolean AWS, Boolean FTP, Boolean FileSystem, List<String> clientsList) throws UnsupportedEncodingException {
		Jedis jedis = pool.getResource();
		try {
			if (!jedis.exists("apps:" + appId)) {
				jedis.hset("apps:" + appId, Application.CREATION_DATE, creationDate);
				jedis.hset("apps:" + appId, Application.CREATION_DATE, creationDate);
				jedis.hset("apps:" + appId, Application.ALIVE, "true");
				jedis.hset("apps:" + appId, Application.APP_NAME, appName);
				jedis.hset("apps:" + appId, Application.APP_KEY, appKey);
				jedis.hset("apps:" + appId, Application.SALT, new String(salt, "ISO-8859-1"));
				jedis.hset("apps:" + appId, Application.HASH, new String(hash, "ISO-8859-1"));
				jedis.hset("apps:" + appId, Application.CONFIRM_USERS_EMAIL, ""+confirmUsersEmail);
				jedis.hset("apps:" + appId, FileMode.aws.toString(), "" + AWS);
				jedis.hset("apps:" + appId, FileMode.ftp.toString(), "" + FTP);
				jedis.hset("apps:" + appId, FileMode.filesystem.toString(), "" + FileSystem);
				if (clientsList != null && clientsList.size()>0){
					Iterator<String> it = clientsList.iterator();
					while(it.hasNext()){
						jedis.lpush("apps_" + appId + "ClientsList_",it.next());
					}
				}
				return getApplication(appId);
			}
		} catch (Exception e) {
		} finally {
			pool.returnResource(jedis);
		}
		return null;
	}
	
	public JSONObject createAppResolutions(JSONObject res, String appId, ModelEnum type) {
		Jedis jedis = pool.getResource();
		try {
			jedis.del("apps:" + appId + ":"+type.toString());
			Iterator<?> keys = res.keys();
			while(keys.hasNext()){
				String key = (String)keys.next();
				String value = res.getString(key);
				jedis.hset("apps:" + appId + ":"+type.toString(), key, value);
			}
			return res;
		} catch (Exception e) {
			Log.error("", this, "createAppImageResolutions", "Error.", e); 
		} finally {
			pool.returnResource(jedis);
		}
		return null;
	}

	// *** UPDATE *** //
	
	public Application updateAppFields(String appId, String alive, String newAppName, Boolean confirmUsersEmail,
			Boolean aws, Boolean ftp, Boolean fileSystem,List<String> clientsList) {
		Jedis jedis = pool.getResource();
		try {
			if (newAppName != null)
				jedis.hset("apps:" + appId, Application.APP_NAME, newAppName);
			if (alive != null)
				jedis.hset("apps:" + appId, Application.ALIVE, alive);
			if (newAppName != null)
				jedis.hset("apps:" + appId, Application.APP_NAME, newAppName);
			if (confirmUsersEmail != null)
				jedis.hset("apps:" + appId, Application.CONFIRM_USERS_EMAIL, ""+confirmUsersEmail);
			if (fileSystem != null && fileSystem)
				aws = ftp = false;
			if (aws != null && aws)
				fileSystem = ftp = false;
			if (ftp != null && ftp)
				fileSystem = aws = false;
			if (aws != null)
				jedis.hset("apps:" + appId, FileMode.aws.toString(), ""+aws);
			if (ftp != null)
				jedis.hset("apps:" + appId, FileMode.ftp.toString(), ""+ftp);
			if (fileSystem != null)
				jedis.hset("apps:" + appId, FileMode.filesystem.toString(), ""+fileSystem);
			if (clientsList != null && clientsList.size()>0){
				jedis.del("apps_" + appId + "ClientsList_");
				Iterator<String> it = clientsList.iterator();
				while(it.hasNext()){
					jedis.lpush("apps_" + appId + "ClientsList_",it.next());
				}
			}
		} finally {
			pool.returnResource(jedis);
		}
		return getApplication(appId);
	}


	// *** GET LIST *** //

	public ArrayList<String> getAllAppIds(Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		// TODO Auto-generated method stub
		return null;
	}

	
	// *** GET *** //

	public String getFileQuality(String appId, ModelEnum type, String key) {
		String res=null;
		if(key!=null){
			Jedis jedis = pool.getResource();
			try {
				if (jedis.exists("apps:" + appId + ":"+type.toString())) {
					res = jedis.hget("apps:" + appId + ":"+type.toString(),key);
				}
			} finally {
				pool.returnResource(jedis);
			}
		}
		return res;
	}
	
	/**
	 * Returns the fields of the corresponding application
	 */
	public Application getApplication(String appId) {
		Jedis jedis = pool.getResource();
		Application res = new Application();
		Map<String, String> fields = null;
		Map<String, String> imageRes = null;
		Map<String, String> videoRes = null;
		Map<String, String> audioRes = null;
		Map<String, String> barsColors = null;
		List<String> clients = null;
		try {
			if (jedis.exists("apps:" + appId)) {
				fields = jedis.hgetAll("apps:" + appId);
				imageRes = jedis.hgetAll("apps:" + appId + ":"+ModelEnum.image);
				videoRes = jedis.hgetAll("apps:" + appId + ":"+ModelEnum.video);
				audioRes = jedis.hgetAll("apps:" + appId + ":"+ModelEnum.audio);
				barsColors = jedis.hgetAll("apps:" + appId + ":"+ModelEnum.bars);
				clients = jedis.lrange("apps_" + appId + "ClientsList_", 0, MAXELEMS);
			}
			if (fields != null) {
				res.setCreationDate(fields.get(Application.CREATION_DATE));
				res.setAlive(fields.get(Application.ALIVE));
				res.setAppName(fields.get(Application.APP_NAME));
				res.setConfirmUsersEmail(Boolean.parseBoolean(fields.get(Application.CONFIRM_USERS_EMAIL)));
				res.setAWS(Boolean.parseBoolean(fields.get(FileMode.aws.toString())));
				res.setFTP(Boolean.parseBoolean(fields.get(FileMode.ftp.toString())));
				res.setFileSystem(Boolean.parseBoolean(fields.get(FileMode.filesystem.toString())));
				res.setUpdateDate(fields.get(Application.CREATION_DATE));
				res.setAppKey(fields.get(Application.APP_KEY));
				res.set_id(appId);
			}
			if(imageRes!=null){
				res.setImageResolutions(imageRes);
			}
			if(videoRes!=null){
				res.setVideoResolutions(videoRes);
			}
			if(audioRes!=null){
				res.setAudioResolutions(audioRes);
			}
			if(barsColors!=null){
				res.setBarsColors(barsColors);
			}
			if(clients!=null && clients.size()>0){
				res.setClients(clients);
			}
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}
	
	/**
	 * Returns the auth fields 
	 */
	public HashMap<String, String> getApplicationAuth(String appId) {
		Jedis jedis = pool.getResource();
		HashMap<String, String> fieldsAuth = new HashMap<String, String>();
		try {
			if (jedis.exists("apps:" + appId)) {
				fieldsAuth.put(Application.HASH, jedis.hget("apps:"+appId, Application.HASH));
				fieldsAuth.put(Application.SALT, jedis.hget("apps:"+appId, Application.SALT));
			}
		} finally {
			pool.returnResource(jedis);
		}
		return fieldsAuth;
	}
	
	public Boolean getConfirmUsersEmail(String appId) {
		Jedis jedis = pool.getResource();
		Boolean confirmUsersEmail = false;
		try {
			confirmUsersEmail = Boolean.parseBoolean(jedis.hget("apps:"+appId, Application.CONFIRM_USERS_EMAIL));
		}finally {
			pool.returnResource(jedis);
		}
		return confirmUsersEmail;
	}

	public FileMode getApplicationFileMode(String appId) {
		Jedis jedis = pool.getResource();
		boolean aws = false;
		boolean ftp = false;
		try {
			aws =  Boolean.parseBoolean(jedis.hget("apps:" + appId, FileMode.aws.toString()));
		} catch (Exception e) { }
		finally {
			pool.returnResource(jedis);
		}
		try {
			ftp = Boolean.parseBoolean(jedis.hget("apps:" + appId, FileMode.ftp.toString()));
		} catch (Exception e) { }
		finally {
			pool.returnResource(jedis);
		}
		
		if (aws) return FileMode.aws;
		else if (ftp) return FileMode.ftp;
		else return FileMode.filesystem;
	}
	
	
	// *** DELETE *** //

	/**
	 * Return codes 1 = Action performed -1 = App does not exist 0 = No action
	 * was performed
	 * 
	 */
	public Boolean deleteApp(String appId) {
		Jedis jedis = pool.getResource();
		Boolean sucess = false;
		try {
			if (jedis.exists("apps:" + appId)) {
				Set<String> inactiveApps = jedis.smembers("apps:inactive");
				Iterator<String> it = inactiveApps.iterator();
				Boolean inactive = false;
				while (it.hasNext() && !inactive) {
					if (it.next().equals(appId))
						inactive = true;
				}
				if (!inactive) {
					jedis.hset("apps:" + appId, Application.ALIVE, "false");
					jedis.sadd("apps:inactive", appId);
					sucess = true;
				}
			}
		} finally {
			pool.returnResource(jedis);
		}
		return sucess;
	}


	// *** EXISTS *** //

	public Boolean appExists(String appId) {
		Jedis jedis = pool.getResource();
		Boolean op;
		try {
			op = jedis.exists("apps:" + appId);
		}finally {
			pool.returnResource(jedis);
		}
		return op;
	}


	// *** OTHERS *** //

	public void reviveApp(String appId) {
		Jedis jedis = pool.getResource();
		try {
			jedis.hset("apps:" + appId, Application.ALIVE, "true");
		} finally {
			pool.returnResource(jedis);
		}
	}

	
	
}
