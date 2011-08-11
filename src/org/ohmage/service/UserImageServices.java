package org.ohmage.service;

import java.util.List;

import org.ohmage.annotator.ErrorCodes;
import org.ohmage.cache.CampaignPrivacyStateCache;
import org.ohmage.cache.CampaignRoleCache;
import org.ohmage.cache.SurveyResponsePrivacyStateCache;
import org.ohmage.dao.CampaignDaos;
import org.ohmage.dao.CampaignImageDaos;
import org.ohmage.dao.DataAccessException;
import org.ohmage.dao.UserCampaignDaos;
import org.ohmage.dao.UserImageDaos;
import org.ohmage.request.Request;

/**
 * This class contains the services that create, read, update, and delete
 * user-image assocations.
 * 
 * @author John Jenkins
 */
public final class UserImageServices {
	/**
	 * Default constructor. Private so that it cannot be instantiated.
	 */
	private UserImageServices() {}
	
	/**
	 * Verifies that an user has sufficient permissions to read an image.
	 * 
	 * @param request The Request that is performing this service.
	 * 
	 * @param requesterUsername The username of the user making this request.
	 * 
	 * @param imageId The unique identifier for the image.
	 * 
	 * @throws ServiceException Thrown if the user doesn't have sufficient
	 * 							permissions to read the image.
	 */
	public static void verifyUserCanReadImage(Request request, String requesterUsername, String imageId) throws ServiceException {
		try {
			// If it is their own image, they can read it.
			if(requesterUsername.equals(UserImageDaos.getImageOwner(imageId))) {
				return;
			}
			
			// Retrieve all of the campaigns associated with an image.
			List<String> campaignIds = CampaignImageDaos.getCampaignIdsForImageId(imageId);
			
			// For each of the campaigns, see if the requesting user has 
			// sufficient permissions.
			for(String campaignId : campaignIds) {
				List<String> roles = UserCampaignDaos.getUserCampaignRoles(requesterUsername, campaignId);

				// If they are a supervisor.
				if(roles.contains(CampaignRoleCache.ROLE_SUPERVISOR)) {
					return;
				}
				
				// Retrieves the privacy state of the image in this campaign. 
				// If null is returned, something has changed since the list of
				// campaign IDs was retrieved, so we need to just error out.
				String imagePrivacyState = CampaignImageDaos.getImagePrivacyStateInCampaign(campaignId, imageId);
				
				// They are an author and the image is shared
				if(roles.contains(CampaignRoleCache.ROLE_AUTHOR) && 
						SurveyResponsePrivacyStateCache.PRIVACY_STATE_SHARED.equals(imagePrivacyState)) {
					return;
				}
				
				// Retrieve the campaign's privacy state.
				String campaignPrivacyState = CampaignDaos.getCampaignPrivacyState(campaignId);
				
				// They are an analyst, the image is shared, and the campaign is shared.
				if(roles.contains(CampaignRoleCache.ROLE_ANALYST) && 
						SurveyResponsePrivacyStateCache.PRIVACY_STATE_SHARED.equals(imagePrivacyState) &&
						CampaignPrivacyStateCache.PRIVACY_STATE_SHARED.equals(campaignPrivacyState)) {
					return;
				}
			}
			
			// If we made it to this point, the requesting user doesn't have
			// sufficient permissions to read the image.
			request.setFailed(ErrorCodes.IMAGE_INSUFFICIENT_PERMISSIONS, "The user doesn't have sufficient permissions to read the image.");
			throw new ServiceException("The user doesn't have sufficient permissions to read the image.");
		}
		catch(DataAccessException e) {
			request.setFailed();
			throw new ServiceException(e);
		}
	}
}