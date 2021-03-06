package infosistema.openbaas.rest;

import infosistema.openbaas.middleLayer.UsersMiddleLayer;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;


public class UserConfirmationResource {

	private UsersMiddleLayer usersMid;
	private String appId;
	private String userId;

	@Context
	UriInfo uriInfo;

	public UserConfirmationResource(UriInfo uriInfo, String appId, String userId) {
		this.usersMid = UsersMiddleLayer.getInstance();
		this.appId = appId;
		this.uriInfo = uriInfo;
		this.userId = userId;
	}

	// *** CREATE *** //
	
	// *** UPDATE *** //
	
	@GET
	public Response confirmEmail(@QueryParam("registrationCode") String registrationCode) {
		Response response = null;
		if (registrationCode != null) {
			String registrationCodeFromDB = usersMid.getUrlUserId(appId, userId);
			if (registrationCodeFromDB != null) {
				if (registrationCodeFromDB.equalsIgnoreCase(registrationCode)) {
					usersMid.removeUrlToUserId(appId, userId);
					usersMid.confirmUserEmail(appId, userId, null);
					response = Response.status(Status.OK).entity("User confirmed, you can now perform operations").build();
				}
			}
		} else {
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		}
		return response;
	}

	// *** DELETE *** //
	
	// *** GET LIST *** //
	
	// *** GET *** //
	
	// *** RESOURCES *** //

	// *** OTHERS *** //
	
}
