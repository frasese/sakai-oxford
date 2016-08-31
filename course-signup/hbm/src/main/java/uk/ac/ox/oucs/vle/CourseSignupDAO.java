/*
 * #%L
 * Course Signup Hibernate
 * %%
 * Copyright (C) 2010 - 2013 University of Oxford
 * %%
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *             http://opensource.org/licenses/ecl2
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package uk.ac.ox.oucs.vle;
// Generated Aug 17, 2010 10:15:40 AM by Hibernate Tools 3.2.2.GA


import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * CourseSignupDAO generated by hbm2java
 */
public class CourseSignupDAO  implements java.io.Serializable {


     /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String id;
    private String userId;
    private CourseSignupService.Status status;
    private Date created;
    private Date amended;
    private Set<CourseComponentDAO> components = new HashSet<CourseComponentDAO>(0);
    private String supervisorId;
    private String department;
    private CourseGroupDAO group;
    private String message;
    private String specialReq;

    public CourseSignupDAO() {
    }

	
    public CourseSignupDAO(String userId) {
        this.userId = userId;
    }
    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return this.userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public CourseSignupService.Status getStatus() {
        return this.status;
    }
    public void setStatus(CourseSignupService.Status status) {
        this.status = status;
    }
    
    public Date getCreated() {
        return this.created;
    }
    public void setCreated(Date created) {
        this.created = created;
    }
    
    public Date getAmended() {
        return this.amended;
    }
    public void setAmended(Date amended) {
        this.amended = amended;
    }
    
    public Set<CourseComponentDAO> getComponents() {
        return this.components;
    }
    public void setComponents(Set<CourseComponentDAO> components) {
        this.components = components;
    }
    
    public String getSupervisorId() {
        return this.supervisorId;
    }
    public void setSupervisorId(String supervisorId) {
        this.supervisorId = supervisorId;
    }
    
    public String getDepartment() {
        return this.department;
    }
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public CourseGroupDAO getGroup() {
        return this.group;
    }
    public void setGroup(CourseGroupDAO group) {
        this.group = group;
    }

	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

    public String getSpecialReq() {
        return specialReq;
    }
    public void setSpecialReq(String specialReq) {
        this.specialReq = specialReq;
    }

}


