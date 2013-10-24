package infosistema.openbaas.rest;

import infosistema.openbaas.middleLayer.AppsMiddleLayer;
import infosistema.openbaas.model.IdsResultSet;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Utils;

import java.util.ArrayList;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;


//@Path("/apps/{appId}/media")
public class MediaResource {

	//	private Map<String, Media> media = new HashMap<String, Media>();
	private String appId;
	private AppsMiddleLayer appsMid;

	static final int idGenerator = 3;

	public MediaResource(AppsMiddleLayer appsMid, String appId) {
		this.appId = appId;
		this.appsMid = appsMid;
	}

	// *** CREATE *** //

	
	// *** UPDATE *** //

	
	// *** DELETE *** //

	
	// *** GET LIST *** //

	//TODO: LOCATION
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
		int code = Utils.treatParameters(ui, hh);
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

	
	// *** GET *** //

	
	// *** DOWNLOAD *** //


	// *** OTHERS *** //

}