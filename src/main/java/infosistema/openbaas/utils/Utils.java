package infosistema.openbaas.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import infosistema.openbaas.dataaccess.models.SessionModel;

public class Utils {
	
	/*
	 * Returns a code corresponding to the sucess or failure Codes: 
	 * -2 -> Forbidden
	 * -1 -> Bad request
	 * 1 ->
	 * sessionExists
	 */
	public static int treatParameters(UriInfo ui, HttpHeaders hh) {
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		Map<String, Cookie> cookiesParams = hh.getCookies();
		int code = -1;
		String userAgent = null;
		String location = null;
		Cookie sessionToken = null;
		try {
			sessionToken = new Cookie(Const.SESSION_TOKEN, headerParams.getFirst(Const.SESSION_TOKEN));
		} catch (Exception e) {
			try {
				sessionToken = cookiesParams.get(Const.SESSION_TOKEN);
			} catch (Exception e2) { }
		}
		if (sessionToken != null) {
			SessionModel sessions = new SessionModel();
			if (sessions.sessionTokenExists(sessionToken.getValue())) {
				code = 1;
				sessions.refreshSession(sessionToken.getValue(), location, new Date().toString(), userAgent);
			} else {
				code = -2;
			}
		}
		return code;
	}
	
	public static int treatParametersAdmin(UriInfo ui, HttpHeaders hh) {
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		Map<String, Cookie> cookiesParams = hh.getCookies();
		int code = -1;
		String userAgent = null;
		String location = null;
		Cookie sessionToken = null;
		try {
			sessionToken = new Cookie(Const.SESSION_TOKEN, headerParams.getFirst(Const.SESSION_TOKEN));
		} catch (Exception e) {
			try {
				sessionToken = cookiesParams.get(Const.SESSION_TOKEN);
			} catch (Exception e2) { }
		}
		if (sessionToken != null && sessionToken.getValue().equals(Const.getADMIN_TOKEN())) {
			SessionModel sessions = new SessionModel();
			if (sessions.sessionTokenExists(sessionToken.getValue())) {
				code = 1;
				sessions.refreshSession(sessionToken.getValue(), location, new Date().toString(), userAgent);
			} else {
				code = -2;
			}
		}
		return code;
	}
	
	public static String getRandomString(int length) {
		return (String) UUID.randomUUID().toString().subSequence(0, length);
	}
	
	public static long roundUp(long num, long divisor) {
	    return (num + divisor - 1) / divisor;
	}

	public static String getSessionToken(HttpHeaders hh) {
		String sessionToken = null; 
		try {
			sessionToken = hh.getRequestHeaders().getFirst(Const.SESSION_TOKEN);
		} catch (Exception e) {
			Log.error("", "infosistema.openbaas.utils.Utils", "getSessionToken", "No session token in request header.", e);
		}
		return sessionToken;
	}
	
	public static Date getDate() {
		return new Date();
	}
	
	public static String printDate(Date date) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return dateFormat.format(date);
	}
	
	public static void printMemoryStats() {
		int mb = 1024*1024;
		 
		//Getting the runtime reference from system
		Runtime runtime = Runtime.getRuntime();
		 
		//Print used memory
		StringBuffer str = new StringBuffer();
		str.append("Used: " + String.valueOf((runtime.totalMemory() - runtime.freeMemory()) / mb));
		str.append(" - Free: " + String.valueOf(runtime.freeMemory() / mb));
		str.append(" - Total: " + String.valueOf(runtime.totalMemory() / mb));
		str.append(" - Max: " + String.valueOf(runtime.maxMemory() / mb));
		Log.info("", null, "Memory - ",str.toString());
	}
}
