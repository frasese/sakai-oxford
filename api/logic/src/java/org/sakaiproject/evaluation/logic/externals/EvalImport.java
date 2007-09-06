/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2005, 2006 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.evaluation.logic.externals;

import java.util.List;

/**
 * Import evaluation data contained in an XML ContentResource
 * 
 * @author rwellis
 *
 */
public interface EvalImport {
	
	/**
	 * Process an XML ContentResource and save/update evaluation data
	 * 
	 * @param id the ContentResource identifier
	 * @param currentUserId the identifier of the current user
	 * @return a List of String messages for the current user
	 */
	public List process(String id, String currentUserId);

}
