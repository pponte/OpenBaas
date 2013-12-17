package infosistema.openbaas.rest;

import infosistema.openbaas.data.Error;
import infosistema.openbaas.data.ListResult;
import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.QueryParameters;
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.middleLayer.AppsMiddleLayer;
import infosistema.openbaas.middleLayer.DocumentMiddleLayer;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.rest.AppResource.PATCH;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Utils;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.codehaus.jettison.json.JSONObject;

public class AppDataResource {

	private DocumentMiddleLayer docMid;
	private String appId;
	private SessionMiddleLayer sessionMid;
	
	@Context
	UriInfo uriInfo;

	public AppDataResource(@Context UriInfo uriInfo, String appId) {
		this.docMid = DocumentMiddleLayer.getInstance();
		this.sessionMid = SessionMiddleLayer.getInstance();
		this.appId = appId;
		this.uriInfo = uriInfo;
	}
	
	// *** CREATE *** //

	/**
	 * Creates the document root, this is treated differently than PUT to
	 * 
	 * @Path("/{pathId:.+ ") due to the Rest resources levels.
	 * 
	 * @param inputJsonObj
	 * @return
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createDocumentRoot(JSONObject inputJsonObj,@Context UriInfo ui, @Context HttpHeaders hh,
			@HeaderParam(value = Const.LOCATION) String location) {
		Response response = null;
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			String sessionToken = Utils.getSessionToken(hh);
			String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
			if (!sessionMid.checkAppForToken(sessionToken, appId))
				return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
			if (AppsMiddleLayer.getInstance().appExists(appId)) {
				if (docMid.insertDocumentInPath(appId, null, null, inputJsonObj, location)) {
					Metadata meta = null;
					while (inputJsonObj.keys().hasNext()) { 
						String key = inputJsonObj.keys().next().toString();
						meta = docMid.createMetadata(appId, null, key, userId, location, inputJsonObj);
					}
					Result res = new Result(inputJsonObj, meta);					
					response = Response.status(Status.OK).entity(res).build();
				} else {
					response = Response.status(Status.BAD_REQUEST).entity(new Error(inputJsonObj.toString())).build();
				}
			} else {
				response = Response.status(Status.NOT_FOUND).entity(new Error(appId)).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}
	

	// *** UPDATE *** //
	
	/**
	 * Create or replace existing elements.
	 * 
	 * @param inputJsonObj
	 * @param path
	 * @return
	 */
	@PUT
	@Path("/{pathId:.+}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createOrReplaceDocument(JSONObject inputJsonObj, @PathParam("pathId") List<PathSegment> path, 
			@Context UriInfo ui,@Context HttpHeaders hh, @HeaderParam(value = Const.LOCATION)String location) {
		Response response = null;
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			String sessionToken = Utils.getSessionToken(hh);
			String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
			if (!sessionMid.checkAppForToken(sessionToken, appId))
				return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
			if (AppsMiddleLayer.getInstance().appExists(appId)) {
				if (docMid.insertDocumentInPath(appId, null, path, inputJsonObj, location)){
					Metadata meta = docMid.createMetadata(appId, null, docMid.convertPath(path), userId, location, inputJsonObj);
					Result res = new Result(inputJsonObj, meta);
					response = Response.status(Status.CREATED).entity(res).build();
				}
				else
					response = Response.status(Status.BAD_REQUEST).entity(new Error(inputJsonObj.toString())).build();
			} else {
				response = Response.status(Status.NOT_FOUND).entity(new Error(appId)).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}

	/**
	 * Partial updates, adds non existing fields and edits existing ones.
	 * 
	 * @param path
	 * @param inputJson
	 * @return
	 */
	@PATCH
	@Path("/{pathId:.+}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response patchDataInElement( @PathParam("pathId") List<PathSegment> path, JSONObject inputJson,
			@Context UriInfo ui, @Context HttpHeaders hh, @HeaderParam(value = Const.LOCATION) String location) {
		Response response = null;
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			String sessionToken = Utils.getSessionToken(hh);
			String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
			if (!sessionMid.checkAppForToken(sessionToken, appId))
				return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
			if (docMid.existsDocumentInPath(appId, null, path)) {
				if (docMid.updateDocumentInPath(appId, null, path, inputJson, location)){
					Metadata meta = docMid.updateMetadata(appId, null, docMid.convertPath(path), userId, location, inputJson);
					Result res = new Result(inputJson, meta);
					response = Response.status(Status.OK).entity(res).build();
				}
				else
					response = Response.status(Status.BAD_REQUEST).entity(new Error(appId)).build();
			} else {
				response = Response.status(Status.NOT_FOUND).entity(new Error(appId)).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}

	
	// *** DELETE *** //
	
	/**
	 * Removes an element and the childs of that element if they exist.
	 * 
	 * @param path
	 * @return
	 */
	@DELETE
	@Path("/{pathId:.+}")
	public Response deleteDataInElement(@PathParam("pathId") List<PathSegment> path, @Context UriInfo ui,
			@Context HttpHeaders hh) {
		Response response = null;
		
		int code = Utils.treatParameters(ui, hh);
		if (!sessionMid.checkAppForToken(Utils.getSessionToken(hh), appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		if (code == 1) {
			if (docMid.existsDocumentInPath(appId, null, path)) {
				if (docMid.deleteDocumentInPath(appId, null, path)){
					Boolean meta = docMid.deleteMetadata(appId, null, docMid.convertPath(path), ModelEnum.data);
					if(meta)
						response = Response.status(Status.OK).entity("").build();
					else
						response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Error("Del Meta")).build();
				}
				else
					response = Response.status(Status.BAD_REQUEST).entity(new Error(path.toString())).build();
			} else {
				response = Response.status(Status.NOT_FOUND).entity(new Error(appId)).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}

	
	// *** GET LIST *** //
	
	/**
	 * Gets all the users in the application.
	 * 
	 * @return
	 */
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response find(@Context UriInfo ui, @Context HttpHeaders hh,
			@QueryParam("query") JSONObject query, @QueryParam(Const.RADIUS) String radiusStr,
			@QueryParam(Const.LAT) String latitudeStr, @QueryParam(Const.LONG) String longitudeStr,
			@QueryParam(Const.PAGE_NUMBER) String pageNumberStr, @QueryParam(Const.PAGE_SIZE) String pageSizeStr, 
			@QueryParam(Const.ORDER_BY) String orderByStr, @QueryParam(Const.ORDER_BY) String orderTypeStr) {
		return findDocument(null, ui, hh, query, radiusStr, latitudeStr, longitudeStr, pageNumberStr, pageSizeStr, orderByStr, orderTypeStr);
	}

	
	/**
	 * Retrieves the data contained in a key.
	 * 
	 * @param path
	 * @return
	 */
	@GET
	@Path("/{pathId:.+}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response findDocument(@PathParam("pathId") List<PathSegment> path, 
			@Context UriInfo ui, @Context HttpHeaders hh,
			JSONObject query, @QueryParam(Const.RADIUS) String radiusStr,
			@QueryParam(Const.LAT) String latitudeStr, @QueryParam(Const.LONG) String longitudeStr,
			@QueryParam(Const.PAGE_NUMBER) String pageNumberStr, @QueryParam(Const.PAGE_SIZE) String pageSizeStr, 
			@QueryParam(Const.ORDER_BY) String orderByStr, @QueryParam(Const.ORDER_BY) String orderTypeStr) {
		Response response = null;
		if (!sessionMid.checkAppForToken(Utils.getSessionToken(hh), appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			if ((latitudeStr != null && longitudeStr != null && radiusStr != null) || query != null) {
				String url = docMid.getDocumentPath(null, path);
				QueryParameters qp = QueryParameters.getQueryParameters(appId, query, radiusStr, latitudeStr, longitudeStr, 
						pageNumberStr, pageSizeStr, orderByStr, orderTypeStr, url, ModelEnum.data);
				try {
					ListResult res = docMid.find(qp);
					response = Response.status(Status.OK).entity(res).build();
				} catch (Exception e) {
					response = Response.status(Status.FORBIDDEN).entity(e.getMessage()).build();
				}
				return response;
			} else if (docMid.existsDocumentInPath(appId, null, path)) {
				String data = docMid.getDocumentInPath(appId, null, path);
				if (data == null)
					response = Response.status(Status.BAD_REQUEST).entity(new Error(appId)).build();
				else{
					Metadata meta = docMid.getMetadata(appId, null, docMid.convertPath(path), ModelEnum.data);
					Result res = new Result(data, meta);
					response = Response.status(Status.OK).entity(res).build();
				}
			} else {
				response = Response.status(Status.NOT_FOUND).entity(new Error(appId)).build();
			}
			
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}


	// *** GET *** //
	
	// *** OTHERS *** //
	
	// *** RESOURCES *** //

}