package infosistema.openbaas.rest;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import infosistema.openbaas.middleLayer.AclMiddleLayer;
import infosistema.openbaas.middleLayer.AppsMiddleLayer;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;


// test MARCIO
public class AclResource {

	List<PathSegment> path;
	AclMiddleLayer aclMid;
	AppsMiddleLayer appsMid;
	private static final String ERROR_TOKEN = "Incorrect session Token";
	private static final String SUCESS_ACL = "Permissions set";

	public AclResource(List<PathSegment> path) {
		this.path = path;
		aclMid = AclMiddleLayer.getInstance();
		appsMid = AppsMiddleLayer.getInstance();
	}

	// *** CREATE *** //

 	// *** UPDATE *** //
	
	// *** DELETE *** //
	
	// *** GET LIST *** //
	
	// *** GET *** //
	
	public String getUserIdFromSessionToken(HttpHeaders hh) {
		Cookie sessionToken = null;
		Map<String, Cookie> cookiesParams = hh.getCookies();
		// iterate cookies
		try {
			sessionToken = cookiesParams.get(Const.SESSION_TOKEN);
		} catch (Exception e) { }
		if (sessionToken != null)
			return SessionMiddleLayer.getInstance().getUserIdUsingSessionToken(sessionToken.getValue());
		else
			return null;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPermissions(@Context HttpHeaders hh) {
		String userId = getUserIdFromSessionToken(hh);
		Response response;
		if (userId == null) {
			response = Response.status(Status.BAD_REQUEST).entity(ERROR_TOKEN)
					.build();
		} else {
			// mandar o path para a camada intermedia e buscar da arvore
			String permissions = aclMid.getPermissions(path, userId);
			JSONObject json = new JSONObject();
			try {
				json.put("permissions", permissions);
			} catch (JSONException e) {
				Log.error("", this, "getPermissions", "Error parsing the JSON.", e); 
			}
			response = Response.status(Status.OK).entity(json).build();
		}
		return response;
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response setPermissions(JSONObject inputJson,
			@Context HttpServletRequest req, @Context UriInfo ui,
			@Context HttpHeaders hh) {
		String userId = getUserIdFromSessionToken(hh);
		Response response;
		if (userId == null) {
			response = Response.status(Status.BAD_REQUEST).entity(ERROR_TOKEN).build();
		} else {
			boolean permissionsError = false;
			String permissions = null;
			try {
				permissions = inputJson.getString("permissions");
			} catch (JSONException e) {
				permissionsError = true;
				Log.error("", this, "setPermissions", "Error parsing the JSON.", e); 
			}
			// only 4 permissions to set CRUD
			if (permissions == null || permissions.length() != 4)
				response = Response.status(Status.BAD_REQUEST)
						.entity("Error with the permissions").build();
			else {
				for (int i = 0; i < permissions.length(); i++) {
					char at = permissions.charAt(i);
					// defined permission character "o,x,-", anything else = error
					// 'o' = permited
					// 'x' = denied
					// '-' = check the parent for this permission
					//order CRUD -> 'xxxx' denied for create,read,update and delete
					//order CRUD -> 'oooo' allowed for create,read,update and delete
					if (at != 'o' && at != 'x' && at != '-') { 
						permissionsError = true;
						break;
					}
				}
				if (permissionsError)
					response = Response.status(Status.BAD_REQUEST)
							.entity("Error with the permissions").build();
				else {
					aclMid.writePermissions(path, permissions, userId);
				}
			}
			response = Response.status(Status.OK).entity(SUCESS_ACL).build();
		}
		return response;
	}
	
	
	// *** OTHERS *** //

	// *** RESOURCES *** //

}
