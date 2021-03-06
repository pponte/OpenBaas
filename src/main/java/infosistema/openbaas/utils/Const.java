package infosistema.openbaas.utils;

import java.util.Properties;

public class Const {

	//CONSTANTS
	
	public static final String SEMICOLON = ";";
	public static final String SESSION_TOKEN = "sessionToken";
	public static final String MESSAGE = "message";
	public static final String FILE = "file";	
	public static final String AUDIO = "audio";
	public static final String VIDEO = "video";
	public static final String IMAGE = "image";
	public static final String LOCATION = "location";	
	public static final String LAT = "lat";	
	public static final String LONG = "long";	
	public static final String RADIUS = "radius";
	public static final String PAGE_NUMBER = "pageNumber";
	public static final String PAGE_SIZE = "pageSize";
	public static final String ELEM_COUNT = "elemCount";
	public static final String ELEM_INDEX = "elemIndex";
	public static final String ORDER_BY = "orderBy";
	public static final String ORDER_TYPE = "orderType";
	public static final String USER_AGENT = "user-agent";
	public static final String USER_ID = "userId";
	public static final String APP_ID = "appId";
	public static final String QUERY = "query";
	
	
	//PROPERTIES
	
	private static Integer DEFAULT_PAGE_SIZE = 10;
	private static Integer DEFAULT_PAGE_NUMBER = 1;
	public static String DEFAULT_ORDER_BY = "_id";
	public static String DEFAULT_ORDER_TYPE = "desc";

	private static String REDIS_GENERAL_USER = "openbaas";
	private static String REDIS_GENERAL_PASS = "redisdbpwd";
	private static String REDIS_GENERAL_SERVER = "localhost";
	private static Integer REDIS_GENERAL_PORT = 6382;

	private static String REDIS_SESSION_USER = "openbaas";
	private static String REDIS_SESSION_PASS = "redisdbpwd";
	private static Integer REDIS_SESSION_PORT = 6380;
	private static String REDIS_SESSION_SERVER = "localhost";

	private static String REDIS_GEO_USER = "openbaas";
	private static String REDIS_GEO_PASS = "redisdbpwd";
	private static String REDIS_GEO_SERVER = "localhost";
	private static Integer REDIS_GEO_PORT = 6381;

	private static String REDIS_CHAT_USER = "openbaas";
	private static String REDIS_CHAT_PASS = "redisdbpwd";
	private static Integer REDIS_CHAT_PORT = 6383;
	private static String REDIS_CHAT_SERVER = "localhost";

	
	private static String MONGO_SERVER = "localhost";
	private static Integer MONGO_PORT = 27017;
	private static String MONGO_DB = "openbaas";
	private static String MONGO_USER = null;
	private static String MONGO_PASS = null;
	
	private static Boolean APNS_PROD = false;
	
	private static int APNS_FEEDBACK_CICLE = -1;
	private static int APNS_PUSH_CICLE = -1;
	
	
	
	private static String EMAIL_CONFIRMATION_ERROR = "Please confirm your email first.";
	private static String EMAIL_CONFIRMATION_SENDED = "Email sent with recovery details.";

	private static Integer IDLENGTH = 3;
	private static Integer PASSWORD_LENGTH = 7;
	private static Double LATITUDE_PRECISION = 10.0;
	private static Double LONGITUDE_PRECISION = 10.0;

	private static String EMAIL_HOST = "mail.infosistema.com";
	private static String EMAIL_PORT = "25";
	private static String EMAIL_AUTH = "true";
	private static String EMAIL_STARTTLS = "true";

	private static String EMAIL_OPENBAAS_EMAIL = "I13005.openbaas@infosistema.com";
	private static String EMAIL_OPENBAAS_EMAIL_PASSWORD = "Infosistema1!";
	private static String EMAIL_SUBJECT_EMAIL_CONFIRMATION = "Email Registry Confirmation";
	private static String EMAIL_SUBJECT_EMAIL_RECOVERY = "Account Recovery";

	private static Integer SESSION_EXPIRETIME = 86400; // 24hours in seconds

	private static String AWS_OPENBAAS_BUCKET = "openbaas";
	private static String AWS_APPMASTERS_GROUP = "ApplicationMasters";

	private static String VIDEO_DEFAULT_RESOLUTION = "360p";
	private static String IMAGE_DEFAULT_SIZE = "300x300";
	private static String AUDIO_DEFAULT_BITRATE = "32";

	private static String LOCAL_STORAGE_PATH = "";
	private static String ADMIN_TOKEN = "~session";

	static {
		loadConstants();
	}

	private static synchronized void loadConstants() {

		try {
			Properties props = LoadProperties.getProperties("infosistema.openbaas.utils.properties.openbaas");

			String stmp = null;

			try {
				DEFAULT_PAGE_SIZE = Integer.parseInt(props.getProperty("DEFAULT_PAGE_SIZE"));
			} catch (Exception e) {}

			try {
				DEFAULT_PAGE_NUMBER = Integer.parseInt(props.getProperty("DEFAULT_PAGE_NUMBER"));
			} catch (Exception e) {}

			stmp = props.getProperty("DEFAULT_ORDER_BY");
			if (stmp != null) DEFAULT_ORDER_BY = stmp;

			stmp = props.getProperty("DEFAULT_ORDER_TYPE");
			if (stmp != null) DEFAULT_ORDER_TYPE = stmp;

			stmp = props.getProperty("REDIS_GENERAL_SERVER");
			if (stmp != null) REDIS_GENERAL_SERVER = stmp;

			try {
				REDIS_GENERAL_PORT = Integer.parseInt(props.getProperty("REDIS_GENERAL_PORT"));
			} catch (Exception e) {}

			try {
				REDIS_SESSION_PORT = Integer.parseInt(props.getProperty("REDIS_SESSION_PORT"));
			} catch (Exception e) {}

			stmp = props.getProperty("REDIS_SESSION_SERVER");
			if (stmp != null) REDIS_SESSION_SERVER = stmp;

			stmp = props.getProperty("REDIS_GEO_SERVER");
			if (stmp != null) REDIS_GEO_SERVER = stmp;

			try {
				REDIS_GEO_PORT = Integer.parseInt(props.getProperty("REDIS_GEO_PORT"));
			} catch (Exception e) {}

			stmp = props.getProperty("REDIS_CHAT_SERVER");
			if (stmp != null) REDIS_CHAT_SERVER = stmp;

			try {
				REDIS_CHAT_PORT = Integer.parseInt(props.getProperty("REDIS_CHAT_PORT"));
			} catch (Exception e) {}

			stmp = props.getProperty("REDIS_GENERAL_USER");
			if (stmp != null) REDIS_GENERAL_USER = stmp;
			
			stmp = props.getProperty("REDIS_GENERAL_PASS");
			if (stmp != null) REDIS_GENERAL_PASS = stmp;
			
			stmp = props.getProperty("REDIS_GEO_USER");
			if (stmp != null) REDIS_GEO_USER = stmp;
			
			stmp = props.getProperty("REDIS_GEO_PASS");
			if (stmp != null) REDIS_GEO_PASS = stmp;
			
			stmp = props.getProperty("REDIS_SESSIONS_USER");
			if (stmp != null) REDIS_SESSION_USER = stmp;
			
			stmp = props.getProperty("REDIS_SESSIONS_PASS");
			if (stmp != null) REDIS_SESSION_PASS = stmp;
			
			stmp = props.getProperty("REDIS_CHAT_USER");
			if (stmp != null) REDIS_CHAT_USER = stmp;
			
			stmp = props.getProperty("REDIS_CHAT_PASS");
			if (stmp != null) REDIS_CHAT_PASS = stmp;
			
			stmp = props.getProperty("MONGO_SERVER");
			if (stmp != null) MONGO_SERVER = stmp;

			try {
				MONGO_PORT = Integer.parseInt(props.getProperty("MONGO_PORT"));
			} catch (Exception e) {}
			
			try {
				setAPNS_PROD(Boolean.parseBoolean(props.getProperty("APNS_PROD")));
			} catch (Exception e) {}
			
			try {
				setAPNS_FEEDBACK_CICLE(Integer.parseInt(props.getProperty("APNS_FEEDBACK_CICLE")));
			} catch (Exception e) {}
			
			try {
				setAPNS_PUSH_CICLE(Integer.parseInt(props.getProperty("APNS_PUSH_CICLE")));
			} catch (Exception e) {}
			
			stmp = props.getProperty("MONGO_DB");
			if (stmp != null) MONGO_DB = stmp;
			
			stmp = props.getProperty("MONGO_USER");
			if (stmp != null) MONGO_USER = stmp;
			
			stmp = props.getProperty("MONGO_PASS");
			if (stmp != null) MONGO_PASS = stmp;

			stmp = props.getProperty("EMAIL_CONFIRMATION_ERROR");
			if (stmp != null) EMAIL_CONFIRMATION_ERROR = stmp;

			stmp = props.getProperty("EMAIL_CONFIRMATION_SENDED");
			if (stmp != null) EMAIL_CONFIRMATION_SENDED = stmp;

			try {
				IDLENGTH = Integer.parseInt(props.getProperty("IDLENGTH"));
			} catch (Exception e) {}

			try {
				PASSWORD_LENGTH = Integer.parseInt(props.getProperty("PASSWORD_LENGTH"));
			} catch (Exception e) {}

			try {
				LATITUDE_PRECISION = Double.parseDouble(props.getProperty("LATITUDE_PRECISION"));
			} catch (Exception e) {}

			try {
				LONGITUDE_PRECISION = Double.parseDouble(props.getProperty("LONGITUDE_PRECISION"));
			} catch (Exception e) {}

			stmp = props.getProperty("EMAIL_HOST");
			if (stmp != null) EMAIL_HOST = stmp;

			stmp = props.getProperty("EMAIL_PORT");
			if (stmp != null) EMAIL_PORT = stmp;

			stmp = props.getProperty("EMAIL_AUTH");
			if (stmp != null) EMAIL_AUTH = stmp;

			stmp = props.getProperty("EMAIL_STARTTLS");
			if (stmp != null) EMAIL_STARTTLS = stmp;

			stmp = props.getProperty("EMAIL_OPENBAAS_EMAIL");
			if (stmp != null) EMAIL_OPENBAAS_EMAIL = stmp;

			stmp = props.getProperty("EMAIL_OPENBAAS_EMAIL_PASSWORD");
			if (stmp != null) EMAIL_OPENBAAS_EMAIL_PASSWORD = stmp;

			stmp = props.getProperty("EMAIL_SUBJECT_EMAIL_CONFIRMATION");
			if (stmp != null) EMAIL_SUBJECT_EMAIL_CONFIRMATION = stmp;

			stmp = props.getProperty("EMAIL_SUBJECT_EMAIL_RECOVERY");
			if (stmp != null) EMAIL_SUBJECT_EMAIL_RECOVERY = stmp;

			try {
				SESSION_EXPIRETIME = Integer.parseInt(props.getProperty("SESSION_EXPIRETIME"));
			} catch (Exception e) {}

			stmp = props.getProperty("AWS_OPENBAAS_BUCKET");
			if (stmp != null) AWS_OPENBAAS_BUCKET = stmp;

			stmp = props.getProperty("AWS_APPMASTERS_GROUP");
			if (stmp != null) AWS_APPMASTERS_GROUP = stmp;

			stmp = props.getProperty("VIDEO_DEFAULT_RESOLUTION");
			if (stmp != null) VIDEO_DEFAULT_RESOLUTION = stmp;

			stmp = props.getProperty("IMAGE_DEFAULT_SIZE");
			if (stmp != null) IMAGE_DEFAULT_SIZE = stmp;

			stmp = props.getProperty("AUDIO_DEFAULT_BITRATE");
			if (stmp != null) AUDIO_DEFAULT_BITRATE = stmp;

			stmp = props.getProperty("LOCAL_STORAGE_PATH");
			if (stmp != null) {
				if (!stmp.endsWith("/")) stmp += "/";
				LOCAL_STORAGE_PATH = stmp;
			}
			
			stmp = props.getProperty("ADMIN_TOKEN");
			if (stmp != null) ADMIN_TOKEN = stmp;
			
		} catch (Throwable t) {
			Log.error("", "Const", "updateConstants", t.getMessage());
		}
	}

	public static Integer getPageSize() {
		return DEFAULT_PAGE_SIZE;
	}
	public static Integer getPageNumber() {
		return DEFAULT_PAGE_NUMBER;
	}

	public static String getOrderBy() {
		return DEFAULT_ORDER_BY;
	}

	public static String getOrderType() {
		return DEFAULT_ORDER_TYPE;
	}

	public static String getRedisGeneralServer() {
		return REDIS_GENERAL_SERVER;
	}

	public static Integer getRedisGeneralPort() {
		return REDIS_GENERAL_PORT;
	}

	public static String getRedisSessionServer() {
		return REDIS_SESSION_SERVER;
	}

	public static Integer getRedisSessionPort() {
		return REDIS_SESSION_PORT;
	}

	public static String getRedisGeoServer() {
		return REDIS_GEO_SERVER;
	}

	public static Integer getRedisGeoPort() {
		return REDIS_GEO_PORT;
	}

	public static String getRedisChatServer() {
		return REDIS_CHAT_SERVER;
	}

	public static Integer getRedisChatPort() {
		return REDIS_CHAT_PORT;
	}


	public static String getMongoServer() {
		return MONGO_SERVER;
	}

	public static Integer getMongoPort() {
		return MONGO_PORT;
	}

	public static String getMongoDb() {
		return MONGO_DB;
	}
	
	public static String getMongoUser() {
		return MONGO_USER;
	}
	
	public static String getMongoPass() {
		return MONGO_PASS;
	}
	
	public static String getRedisChatUser() {
		return REDIS_CHAT_USER;
	}
	
	public static String getRedisChatPass() {
		return REDIS_CHAT_PASS;
	}
	
	public static String getRedisSessionUser() {
		return REDIS_SESSION_USER;
	}
	
	public static String getRedisSessionPass() {
		return REDIS_SESSION_PASS;
	}
	public static String getRedisGeoUser() {
		return REDIS_GEO_USER;
	}
	
	public static String getRedisGEOPass() {
		return REDIS_GEO_PASS;
	}
	public static String getRedisGeneralUser() {
		return REDIS_GENERAL_USER;
	}
	
	public static String getRedisGeneralPass() {
		return REDIS_GENERAL_PASS;
	}
	
	public static Boolean getMongoAuth() {
		if(getMongoPass()!=null && getMongoUser() != null)
			return true;
		else
			return false;
	}
	
	public static Boolean getRedisGeneralAuth() {
		if(getRedisGeneralPass() !=null && getRedisGeneralUser() != null)
			return true;
		else
			return false;
	}
	
	public static Boolean getRedisSessionAuth() {
		if(getRedisSessionPass() !=null && getRedisSessionUser() != null)
			return true;
		else
			return false;
	}

	public static String getEmailConfirmationError() {
		return EMAIL_CONFIRMATION_ERROR;
	}

	public static String getEmailConfirmationSended() {
		return EMAIL_CONFIRMATION_SENDED;
	}

	public static Integer getIdLength() {
		return IDLENGTH;
	}

	public static Integer getPasswordLength() {
		return PASSWORD_LENGTH;
	}

	public static Double getLatitudePrecision() {
		return LATITUDE_PRECISION;
	}

	public static Double getLongitudePrecision() {
		return LONGITUDE_PRECISION;
	}

	public static String getEmailHost() {
		return EMAIL_HOST;
	}

	public static String getEmailPort() {
		return EMAIL_PORT;
	}

	public static String getEmailAuth() {
		return EMAIL_AUTH;
	}

	public static String getEmailStartTLS() {
		return EMAIL_STARTTLS;
	}

	public static String getEmailOpenBaasEmail() {
		return EMAIL_OPENBAAS_EMAIL;
	}

	public static String getEmailOpenBaasEmailPassword() {
		return EMAIL_OPENBAAS_EMAIL_PASSWORD;
	}

	public static String getEmailSubjectEmailConfirmation() {
		return EMAIL_SUBJECT_EMAIL_CONFIRMATION;
	}

	public static String getEmailSubjectEmailRecovery() {
		return EMAIL_SUBJECT_EMAIL_RECOVERY;
	}

	public static Integer getSessionExpireTime() {
		return SESSION_EXPIRETIME;
	}

	public static String getAwsOpenBaasBucket() {
		return AWS_OPENBAAS_BUCKET;
	}

	public static String getAwsAppMastersGroup() {
		return AWS_APPMASTERS_GROUP;
	}

	public static String getVideoDefaultResolution() {
		return VIDEO_DEFAULT_RESOLUTION;
	}

	public static String getImageDefaultSize() {
		return IMAGE_DEFAULT_SIZE;
	}

	public static String getAudioDegaultBitrate() {
		return AUDIO_DEFAULT_BITRATE;
	}

	public static String getLocalStoragePath() {
		return LOCAL_STORAGE_PATH;
	}

	public static String getADMIN_TOKEN() {
		return ADMIN_TOKEN;
	}

	public static void setADMIN_TOKEN(String aDMIN_TOKEN) {
		ADMIN_TOKEN = aDMIN_TOKEN;
	}

	public static Boolean getAPNS_PROD() {
		return APNS_PROD;
	}

	public static void setAPNS_PROD(Boolean aPNS_PROD) {
		APNS_PROD = aPNS_PROD;
	}
	
	public static int getAPNS_FEEDBACK_CICLE() {
		return APNS_FEEDBACK_CICLE;
	}
	public static int getAPNS_PUSH_CICLE() {
		return APNS_PUSH_CICLE;
	}

	public static void setAPNS_FEEDBACK_CICLE(int aPNS_FEEDBACK_CICLE) {
		APNS_FEEDBACK_CICLE = aPNS_FEEDBACK_CICLE;
	}
	public static void setAPNS_PUSH_CICLE(int aPNS_PUSH_CICLE) {
		APNS_PUSH_CICLE = aPNS_PUSH_CICLE;
	}
	
}
