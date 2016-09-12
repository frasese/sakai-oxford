package org.sakaiproject.user.detail;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.CandidateDetailProvider;
import org.sakaiproject.user.api.User;

public class CandidateDetailProviderImpl implements CandidateDetailProvider {
	
	private static final String USER_PROP_CANDIDATE_ID = "candidateID";
	private static final String USER_PROP_ADDITIONAL_INFO = "additionalInfo";
	
	private final static String SITE_PROP_USE_INSTITUTIONAL_ANONYMOUS_ID = "useInstitutionalAnonymousID";
	private final static String SITE_PROP_DISPLAY_ADDITIONAL_INFORMATION = "displayAdditionalInformation";
	
	private final static String SYSTEM_PROP_USE_INSTITUTIONAL_ANONYMOUS_ID = "useInstitutionalAnonymousID";
	private final static String SYSTEM_PROP_DISPLAY_ADDITIONAL_INFORMATION = "displayAdditionalInformation";

	private static Log M_log = LogFactory.getLog(CandidateDetailProviderImpl.class);
	
	private ServerConfigurationService serverConfigurationService;
	private SiteService siteService;
	private ToolManager toolManager;
	
	public void init() {
		Objects.requireNonNull(siteService, "SiteService must be set");
		Objects.requireNonNull(toolManager, "ToolManager must be set");
		Objects.requireNonNull(serverConfigurationService, "ServerConfigurationService must be set");
	}
	
	public Optional<String> getCandidateID(User user, Site site) {
		try {
			if(user != null) {
				//check if we should use the institutional anonymous id (system-wide or site-based)
				if(useInstitutionalAnonymousId(site)) {
					if(user.getProperties() != null && StringUtils.isNotBlank(user.getProperties().getProperty(USER_PROP_CANDIDATE_ID)) && StringUtils.isNotBlank(ValueEncryptionUtilities.decrypt(user.getProperties().getProperty(USER_PROP_CANDIDATE_ID)))) {
						//this property is encrypted, so we need to decrypt it
						return Optional.ofNullable(ValueEncryptionUtilities.decrypt(user.getProperties().getProperty(USER_PROP_CANDIDATE_ID)));
					}
				}
			}
		} catch(Exception e) {
			M_log.warn("Error getting candidateID for "+((user != null) ? user.getId() : "-null-"), e);
		}
		return Optional.empty();
	}
	
	public boolean useInstitutionalAnonymousId(Site site) {
		try {
			return (serverConfigurationService.getBoolean(SYSTEM_PROP_USE_INSTITUTIONAL_ANONYMOUS_ID, false) || (site != null && Boolean.parseBoolean(site.getProperties().getProperty(SITE_PROP_USE_INSTITUTIONAL_ANONYMOUS_ID))));
		} catch(Exception ignore) {}
		return false;
	}
	
	public Optional<List<String>> getAdditionalNotes(User user, Site site) {
		try {
			if(user != null) {
				//check if additional notes is enabled (system-wide or site-based)
				if(isAdditionalNotesEnabled(site)) {
					if(user.getProperties() != null && user.getProperties().getPropertyList(USER_PROP_ADDITIONAL_INFO) != null) {
						List<String> ret = new ArrayList<String>();
						for(String s : user.getProperties().getPropertyList(USER_PROP_ADDITIONAL_INFO)) {
							//this property is encrypted, so we need to decrypt it
							if(StringUtils.isNotBlank(s) && StringUtils.isNotBlank(ValueEncryptionUtilities.decrypt(s))){
								ret.add(ValueEncryptionUtilities.decrypt(s));
							}
						}
						//return Optional.ofNullable(user.getProperties().getProperty(USER_PROP_ADDITIONAL_INFO));
						return Optional.ofNullable(ret);
					}
				}
			}
		} catch(Exception e) {
			M_log.warn("Error getting additional info for "+((user != null) ? user.getId() : "-null-"), e);
		}
		return Optional.empty();
	}
	
	public boolean isAdditionalNotesEnabled(Site site) {
		try {
			return (serverConfigurationService.getBoolean(SYSTEM_PROP_DISPLAY_ADDITIONAL_INFORMATION, false) || (site != null && Boolean.parseBoolean(site.getProperties().getProperty(SITE_PROP_DISPLAY_ADDITIONAL_INFORMATION))));
		} catch(Exception ignore) {}
		return false;
	}
	

	public void setServerConfigurationService(ServerConfigurationService serverConfigurationService) {
		this.serverConfigurationService = serverConfigurationService;
	}
	
	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}

	public void setToolManager(ToolManager toolManager) {
		this.toolManager = toolManager;
	}
}