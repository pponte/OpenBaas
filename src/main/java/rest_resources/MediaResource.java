package rest_resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import resourceModelLayer.AppsMiddleLayer;
import rest_Models.IdsResultSet;
import utils.Const;

//@Path("/apps/{appId}/media")
public class MediaResource {

//	private Map<String, Media> media = new HashMap<String, Media>();
	private String appId;
	private AppsMiddleLayer appsMid;
	static final int idGenerator = 3;
	
	public MediaResource(AppsMiddleLayer appsMid, String appId) {
		this.appId = appId;
		this.appsMid = appsMid;
		
		//String id = getRandomString(idGenerator);
	}
	/*
	 * Returns a code corresponding to the sucess or failure Codes: 
	 * -2 -> Forbidden
	 * -1 -> Bad request
	 * 1 ->
	 * sessionExists
	 */
	private int treatParameters(UriInfo ui, HttpHeaders hh) {
		/*MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
		MultivaluedMap<String, String> pathParams = ui.getPathParameters();*/
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		Map<String, Cookie> cookiesParams = hh.getCookies();
		int code = -1;
		List<String> location = null;
		Cookie sessionToken = null;
		List<String> userAgent = null;
		// iterate cookies
		for (Entry<String, Cookie> entry : cookiesParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("sessionToken"))
				sessionToken = entry.getValue();
		}
		// iterate headers
		for (Entry<String, List<String>> entry : headerParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("sessionToken"))
				sessionToken = new Cookie("sessionToken", entry.getValue().get(0));
			if (entry.getKey().equalsIgnoreCase("location"))
				location = entry.getValue();
			else if (entry.getKey().equalsIgnoreCase("user-agent"))
				userAgent = entry.getValue();
		}
		if (sessionToken != null) {
			if (appsMid.sessionTokenExists(sessionToken.getValue())) {
				code = 1;
				if (location != null) {
					appsMid.refreshSession(sessionToken.getValue(),
							location.get(0), userAgent.get(0));
				} else
					appsMid.refreshSession(sessionToken.getValue());
			}else{
				code = -2;
			}
		}
		return code;
	}
	/*private String getRandomString(int length) {
		return (String) UUID.randomUUID().toString().subSequence(0, length);
	}*/

	//TODO: PAGINATION
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response findAll(@Context UriInfo ui, @Context HttpHeaders hh,
			@QueryParam("pageNumber") Integer pageNumber, @QueryParam("pageSize") Integer pageSize, 
			@QueryParam("orderBy") String orderBy, @QueryParam("orderType") String orderType ) {
		if (pageNumber == null) pageNumber = Const.PAGE_NUMBER;
		if (pageSize == null) 	pageSize = Const.PAGE_SIZE;
		if (orderBy == null) 	orderBy = Const.ORDER_BY;
		if (orderType == null) 	orderType = Const.ORDER_TYPE;
		Response response = null;
		int code = this.treatParameters(ui, hh);
		if (code == 1) {
			if(appsMid.appExists(appId)){
				ArrayList<String> mediaIds = appsMid.getAllMediaIds(appId,pageNumber,pageSize,orderBy,orderType);
				IdsResultSet res = new IdsResultSet(mediaIds,pageNumber);
				response = Response.status(Status.OK).entity(res).build();
			}else{
				response = Response.status(Status.NOT_FOUND).entity(appId).build();
			}
			
		}else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.")
			 .build();
		return response;
	}
}
