package infosistema.openbaas.middleLayer;

import infosistema.openbaas.dataaccess.models.NotificationsModel;


public class NotificationMiddleLayer {

	private NotificationsModel noteModel;


	// *** INSTANCE *** //
	private static NotificationMiddleLayer instance = null;
	
	private NotificationMiddleLayer() {
		super();
		noteModel = new NotificationsModel();
	}
	
	public static NotificationMiddleLayer getInstance() {
		if (instance == null) instance = new NotificationMiddleLayer();
		return instance;
	}
	
	
	//TODO JM
	
	
	
	
	
}
