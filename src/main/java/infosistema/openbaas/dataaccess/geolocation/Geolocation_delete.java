package infosistema.openbaas.dataaccess.geolocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.ValueComparator;
import infosistema.openbaas.utils.geolocation.Geo;

public class Geolocation_delete {
	
	Geo geo = Geo.getInstance();

	private static final String OBJECTID_FORMAT = "%s:%s:%s"; // latitude;longitude;objectid
	private static final String GRID_FORMAT = "%s:%s:%s:%s"; // latitude;longitude;appid;objecttype
	
	// *** AUX *** //
	
	private String getObjectId(double latitude, double longitude, String objectId) {
		return String.format(OBJECTID_FORMAT, latitude, longitude, objectId);
	}
	
	private String getGridSquareId(double latitude, double longitude, String appId, ModelEnum objectType) {
		String aux = String.format(GRID_FORMAT, latitude, longitude, appId, objectType.toString());
		//String twoPoints = ":";
		//String aux = latitude+twoPoints+longitude+twoPoints+appId+twoPoints+objectType;	
		return aux;
	}


	// *** CREATE *** //
	
	public boolean insertObjectInGrid(double latitude, double longitude, ModelEnum type, String appId, String objectId) {
		String gridObjectId = getObjectId(latitude, longitude, objectId);

		double gridLatitude = geo.getGridLatitude(latitude);
		double gridLongitude = geo.getGridLongitude(longitude);

		String gridSquareId = getGridSquareId(gridLatitude, gridLongitude, appId, type);

		return insert(gridSquareId, gridObjectId);
	}

	//private
	private boolean insert(String gridSquareId, String gridObjectId) {
		Boolean success = false;
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisGeoServer(), Const.getRedisGeoPort());
		Jedis jedis = pool.getResource();
		try{
			jedis.sadd(gridSquareId, gridObjectId);
			success = true;
		} catch (Exception e) {
			Log.error("", this, "insert", "An error ocorred.", e); 
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return success;
	}
	

	// *** UPDATE *** //
	
	public boolean updateObjectInGrid(double srcLatitude, double srcLongitude, double destLatitude, double destLongitude, ModelEnum type, String appId, String objectId) {
		String srcGridObjectId = getObjectId(srcLatitude, srcLongitude, objectId);
		String destGridObjectId = getObjectId(destLatitude, destLongitude, objectId);

		double srcGridLatitude = geo.getGridLatitude(srcLatitude);
		double srcGridLongitude = geo.getGridLongitude(srcLongitude);
		double destGridLatitude = geo.getGridLatitude(destLatitude);
		double destGridLongitude = geo.getGridLongitude(destLongitude);

		String srcGridSquareId = getGridSquareId(srcGridLatitude, srcGridLongitude, appId, type);
		String destGridSquareId = getGridSquareId(destGridLatitude, destGridLongitude, appId, type);

		delete(srcGridSquareId, srcGridObjectId);
		insert(destGridSquareId, destGridObjectId);
		
		return true;
	}
	

	// *** DELETE *** //
	
	public boolean deleteObjectFromGrid(double latitude, double longitude, ModelEnum type, String appId, String objectId) {
		String gridObjectId = getObjectId(latitude, longitude, objectId);

		double gridLatitude = geo.getGridLatitude(latitude);
		double gridLongitude = geo.getGridLongitude(longitude);

		String gridSquareId = getGridSquareId(gridLatitude, gridLongitude, appId, type);

		return delete(gridSquareId, gridObjectId);
	}
	
	//private
	private boolean delete(String gridSquareId, String gridObjectId) {
		Boolean success = false;
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisGeoServer(), Const.getRedisGeoPort());
		Jedis jedis = pool.getResource();
		try{
			jedis.srem(gridSquareId, gridObjectId);
			success = true;
		}catch(Exception e){
			Log.error("", this, "delete", "An error ocorred.", e); 
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return success;
	}

	
	// *** GET LIST *** //

	public ArrayList<String> getObjectsInGrid(double latitudeIni, double longitudeIni, double latitudeEnd, double longitudeEnd, String appId, ModelEnum type) {
		ArrayList<String> retObj = new ArrayList<String>();
		double gridLatitudeIni = geo.getGridLatitude(latitudeIni);
		double gridLongitudeIni = geo.getGridLongitude(longitudeIni);
		double gridLatitudeEnd = geo.getGridLatitude(latitudeEnd);
		double gridLongitudeEnd = geo.getGridLongitude(longitudeEnd);

		while (gridLatitudeIni <= gridLatitudeEnd) {
			while (gridLongitudeIni <= gridLongitudeEnd) {
				String gridSquareId = getGridSquareId(gridLatitudeIni, gridLongitudeIni, appId, type);
				retObj.addAll(getObjectsIn(gridSquareId));
				gridLongitudeIni += Const.getLongitudePrecision();
			}
			gridLatitudeIni += Const.getLatitudePrecision();
		}
		return retObj;
	}

	public ArrayList<String> getObjectsInDistance(double latitude, double longitude, double radius, String appId, ModelEnum type) {
		ArrayList<String> retObj = new ArrayList<String>(); 

		double latitudeIni = latitude-geo.transformMetersInDegreesLat(radius); 
		double latitudeEnd = latitude+geo.transformMetersInDegreesLat(radius); 
		double longitudeIni = longitude-geo.transformMetersInDegreesLong(radius, latitudeIni); 
		double longitudeEnd = longitude+geo.transformMetersInDegreesLong(radius, latitudeEnd); 
		ArrayList<String> objectsInGrid = getObjectsInGrid(latitudeIni, longitudeIni, latitudeEnd, longitudeEnd, appId, type);
		HashMap<String,Double> elementsToOrder = new HashMap<String, Double>();
		for (String object : objectsInGrid) {
			String[] objectArray = object.split(":");
			Double objLatitude = Double.parseDouble(objectArray[0]);
			Double objLongitude = Double.parseDouble(objectArray[1]);
			String objId = objectArray[2];
			double dist2Or = geo.getDistanceFromLatLonInKm(latitude, longitude, objLatitude, objLongitude);
			if( dist2Or <= (radius / 1000)) {
				elementsToOrder.put(objId, dist2Or);
		    }
		}
		retObj = orderHash(elementsToOrder);

		return retObj;
	}

	//private
	private Set<String> getObjectsIn(String gridSquareId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisGeoServer(), Const.getRedisGeoPort());
		Jedis jedis = pool.getResource();
		Set<String> retObj = new TreeSet<String>();
		try {
			//if (jedis.exists("sessions:" + sessionId))
			retObj = jedis.smembers(gridSquareId);
		} finally {
			pool.returnResource(jedis);
			pool.destroy();
		}
		return retObj;
	}
	
	private ArrayList<String> orderHash(HashMap<String,Double> hash2Order) {
	{  
			ArrayList<String> res = new ArrayList<String>();
	        ValueComparator bvc =  new ValueComparator(hash2Order);
	        TreeMap<String,Double> sorted_map = new TreeMap<String,Double>(bvc);
	        sorted_map.putAll(hash2Order);
	        for (Map.Entry<String,Double> entry : sorted_map.entrySet()) {
	        	res.add(entry.getKey());
	        }
	        return res ;
	    }
    } 

}
