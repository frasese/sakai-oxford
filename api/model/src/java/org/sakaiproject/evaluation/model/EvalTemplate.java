package org.sakaiproject.evaluation.model;

// Generated Mar 20, 2007 10:08:13 AM by Hibernate Tools 3.2.0.beta6a

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * EvalTemplate generated by hbm2java
 */
public class EvalTemplate implements java.io.Serializable {

	// Fields    

	private Long id;
	
	private String eid;

	private Date lastModified;

	private String owner;

	private String type;

	private String title;

	private String description;

	private String sharing;

	private Boolean expert;

	private String expertDescription;

	private Set templateItems = new HashSet(0);

	private Boolean locked;

	// Constructors

	/** default constructor */
	public EvalTemplate() {
	}

	/** minimal constructor */
	public EvalTemplate(Date lastModified, String owner, String type, String title, String sharing, Boolean expert) {
		this.lastModified = lastModified;
		this.owner = owner;
		this.type = type;
		this.title = title;
		this.sharing = sharing;
		this.expert = expert;
	}

	/** full constructor */
	public EvalTemplate(Date lastModified, String owner, String type, String title, String description, String sharing, Boolean expert,
			String expertDescription, Set templateItems, Boolean locked) {
		this.lastModified = lastModified;
		this.owner = owner;
		this.type = type;
		this.title = title;
		this.description = description;
		this.sharing = sharing;
		this.expert = expert;
		this.expertDescription = expertDescription;
		this.templateItems = templateItems;
		this.locked = locked;
	}

	// Property accessors
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public String getEid() {
		return this.eid;
	}

	public void setEid(String eid) {
		this.eid = eid;
	}

	public Date getLastModified() {
		return this.lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public String getOwner() {
		return this.owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSharing() {
		return this.sharing;
	}

	public void setSharing(String sharing) {
		this.sharing = sharing;
	}

	public Boolean getExpert() {
		return this.expert;
	}

	public void setExpert(Boolean expert) {
		this.expert = expert;
	}

	public String getExpertDescription() {
		return this.expertDescription;
	}

	public void setExpertDescription(String expertDescription) {
		this.expertDescription = expertDescription;
	}

	public Set getTemplateItems() {
		return this.templateItems;
	}

	public void setTemplateItems(Set templateItems) {
		this.templateItems = templateItems;
	}

	public Boolean getLocked() {
		return this.locked;
	}

	public void setLocked(Boolean locked) {
		this.locked = locked;
	}

}
