/******************************************************************************
 * EvalEvaluationsLogicImpl.java - created by aaronz@vt.edu on Dec 26, 2006
 * 
 * Copyright (c) 2006 Virginia Polytechnic Institute and State University
 * Licensed under the Educational Community License version 1.0
 * 
 * A copy of the Educational Community License has been included in this 
 * distribution and is available at: http://www.opensource.org/licenses/ecl1.php
 * 
 * Contributors:
 * Aaron Zeckoski (aaronz@vt.edu) - primary
 * 
 *****************************************************************************/

package org.sakaiproject.evaluation.logic.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.evaluation.dao.EvaluationDao;
import org.sakaiproject.evaluation.logic.EvalEvaluationsLogic;
import org.sakaiproject.evaluation.logic.EvalExternalLogic;
import org.sakaiproject.evaluation.logic.EvalSettings;
import org.sakaiproject.evaluation.logic.impl.interceptors.EvaluationInterceptor;
import org.sakaiproject.evaluation.logic.impl.interceptors.EvaluationModificationRegistry;
import org.sakaiproject.evaluation.logic.model.Context;
import org.sakaiproject.evaluation.model.EvalAssignContext;
import org.sakaiproject.evaluation.model.EvalEvaluation;
import org.sakaiproject.evaluation.model.EvalResponse;
import org.sakaiproject.evaluation.model.EvalTemplate;
import org.sakaiproject.evaluation.model.constant.EvalConstants;
import org.sakaiproject.evaluation.model.utils.EvalUtils;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.target.SingletonTargetSource;

/**
 * Implementation for EvalEvaluationsLogic
 * (Note for developers - do not modify this without permission from the author)<br/>
 * 
 * TODO - Need a way to make sure the states of the evaluation are adjusted correctly
 * (using some kind of hourly check or maybe a scheduler), also need to put in the logic
 * to handle locking and unlocking correctly
 *
 * @author Aaron Zeckoski (aaronz@vt.edu)
 */
public class EvalEvaluationsLogicImpl implements EvalEvaluationsLogic {

	private static Log log = LogFactory.getLog(EvalEvaluationsLogicImpl.class);

	private EvaluationDao dao;
	public void setDao(EvaluationDao dao) {
		this.dao = dao;
	}

	private EvalExternalLogic external;
	public void setExternalLogic(EvalExternalLogic external) {
		this.external = external;
	}

	private EvalSettings settings;
	public void setSettings(EvalSettings settings) {
		this.settings = settings;
	}


	// INIT method
	public void init() {
		log.debug("Init");
	}

    // Non-API method used from interceptor.
    // Semantics - throws exception on forbidden operation.
	public void modifyEvaluation(EvalEvaluation evaluation, String method) {
      if (method.startsWith("set")) {
        String userId = external.getCurrentUserId();
        // check the user control permissions
        if (! canUserControlEvaluation(userId, evaluation) ) {
            throw new SecurityException("User ("+userId+") attempted to update existing evaluation ("+evaluation.getId()+") without permissions");
        }
        String state = EvalUtils.getEvaluationState(evaluation);        
        
        String property = Character.toLowerCase(method.charAt(3)) + method.substring(4);
        if (!EvaluationModificationRegistry.isPermittedModification(state, property)) {
          throw new IllegalArgumentException("Cannot change state of evaluation with " +
              method + " when it is in state " + state);
        }
      }
    }
	// EVALUATIONS

	/* (non-Javadoc)
	 * @see edu.vt.sakai.evaluation.logic.EvalEvaluationsLogic#saveEvaluation(edu.vt.sakai.evaluation.model.EvalEvaluation, java.lang.String)
	 */
	public void saveEvaluation(EvalEvaluation evaluation, String userId) {
		log.debug("evalId: " + evaluation.getId() + ",userId: " + userId);

		// set the date modified
		evaluation.setLastModified( new Date() );

		// test date ordering first
		if (evaluation.getStartDate().compareTo(evaluation.getDueDate()) >= 0 ) {
			throw new IllegalArgumentException(
					"due date (" + evaluation.getDueDate() +
					") must occur after start date (" + 
					evaluation.getStartDate() + "), can occur on the same date but not at the same time");
		} else if (evaluation.getDueDate().compareTo(evaluation.getStopDate()) < 0 ) {
			throw new IllegalArgumentException(
					"stop date (" + evaluation.getStopDate() +
					") must occur on or after due date (" + 
					evaluation.getDueDate() + "), can be identical");
		} else if (evaluation.getViewDate().compareTo(evaluation.getStopDate()) <= 0 ) {
			throw new IllegalArgumentException(
					"view date (" + evaluation.getViewDate() +
					") must occur after stop date (" + 
					evaluation.getStopDate() + "), can occur on the same date but not at the same time");
		}

		// now perform checks depending on whether this is new or existing
		Calendar calendar = GregorianCalendar.getInstance();
		calendar.add(Calendar.MINUTE, -15); // put today a bit in the past (15 minutes)
		Date today = calendar.getTime();
		if (evaluation.getId() == null) { // creating new evaluation
			// test if new evaluation occurs in the past
			if (evaluation.getStartDate().before(today)) {
				throw new IllegalArgumentException(
						"start date (" + evaluation.getStartDate() +
						") cannot occur in the past for new evaluations");
			} else if (evaluation.getDueDate().before(today)) {
				throw new IllegalArgumentException(
						"due date (" + evaluation.getDueDate() +
						") cannot occur in the past for new evaluations");
			} else if (evaluation.getStopDate().before(today)) {
				throw new IllegalArgumentException(
						"stop date (" + evaluation.getStopDate() +
						") cannot occur in the past for new evaluations");
			}

			// make sure the state is set correctly
			fixReturnEvalState(evaluation, false);

			// check user permissions (uses public method)
			if (! canBeginEvaluation(userId) ) {
				throw new SecurityException("User ("+userId+") attempted to create evaluation without permissions");
			}

		} else { // updating existing evaluation
		  if (evaluation.getClass() == EvalEvaluation.class) {
            throw new IllegalStateException("Attempt to save non-persistent instance of Evaluation with id " + evaluation.getId() + 
                ": to continue working with this entity you must refetch it using getEvaluationById");      
          }
          // All other checks have been moved to interceptor
		}

		// make sure we are not using a blank template here
		if (evaluation.getTemplate() == null ||
				evaluation.getTemplate().getTemplateItems() == null ||
				evaluation.getTemplate().getTemplateItems().size() <= 0) {
			throw new IllegalArgumentException("Evaluations must include a template and the template must have at least one item in it");
		}

		// TODO - fill in any default values and nulls here
		if (evaluation.getLocked() == null) {
			evaluation.setLocked( Boolean.FALSE );
		}

		// TODO - add system setting checks for things like allowing users to modify responses
		if (evaluation.getBlankResponsesAllowed() == null) {
			evaluation.setBlankResponsesAllowed( Boolean.TRUE );
		}
		Boolean systemBlankResponses = (Boolean) settings.get( EvalSettings.STUDENT_ALLOWED_LEAVE_UNANSWERED );
		if ( systemBlankResponses != null ) {
			evaluation.setBlankResponsesAllowed( systemBlankResponses );
		}

		// TODO - update the states of templates used in this evaluation to locked when this is locked

		dao.save(evaluation);
		log.info("User ("+userId+") saved evaluation ("+evaluation.getId()+"), title: " + evaluation.getTitle());	}

	/* (non-Javadoc)
	 * @see edu.vt.sakai.evaluation.logic.EvalEvaluationsLogic#deleteEvaluation(java.lang.Long, java.lang.String)
	 */
	public void deleteEvaluation(Long evaluationId, String userId) {
		log.debug("evalId: " + evaluationId + ",userId: " + userId);
		EvalEvaluation eval = (EvalEvaluation) dao.findById(EvalEvaluation.class, evaluationId);
		if (eval == null) {
			log.warn("Cannot find evaluation to delete with id: " + evaluationId);
			return;
		}

		if ( canUserRemoveEval(userId, eval) ) {
			Set[] entitySets = new HashSet[3];
			// remove associated AssignContexts
			List acs = dao.findByProperties(EvalAssignContext.class, 
					new String[] {"evaluation.id"}, 
					new Object[] {evaluationId});
			Set assignSet = new HashSet(acs);
			entitySets[0] = assignSet;

			// remove associated unused email templates
			Set emailSet = new HashSet();
			entitySets[1] = emailSet;
			if (eval.getAvailableEmailTemplate() != null) {
				if (eval.getAvailableEmailTemplate().getDefaultType() == null) {
					// only remove non-default templates
					Long emailTemplateId = eval.getAvailableEmailTemplate().getId();
					if ( dao.countByProperties(EvalEvaluation.class, 
							new String[] {"availableEmailTemplate.id"}, 
							new Object[] {emailTemplateId}) <= 1 ) {
						// template was only used in this evaluation
						emailSet.add( eval.getAvailableEmailTemplate() );
					}
				}
			}
			if (eval.getReminderEmailTemplate() != null) {
				if (eval.getReminderEmailTemplate().getDefaultType() == null) {
					Long emailTemplateId = eval.getReminderEmailTemplate().getId();
					if ( dao.countByProperties(EvalEvaluation.class, 
							new String[] {"reminderEmailTemplate.id"}, 
							new Object[] {emailTemplateId}) <= 1 ) {
						// template was only used in this evaluation
						emailSet.add( eval.getReminderEmailTemplate() );
					}
				}
			}

			// add eval to a set to be removed
			Set evalSet = new HashSet();
			entitySets[2] = evalSet;
			evalSet.add(eval);

			// TODO - update the states of templates used in this evaluation to unlocked (if not used elsewhere)
			log.error("Locking templates not implemented yet");

			// remove the evaluation and related items in one transaction
			dao.deleteMixedSet(entitySets);
			//dao.delete(eval);
			log.info("User ("+userId+") removed evaluation ("+evaluationId+"), title: " + eval.getTitle());
			return;
		}

		// should not get here so die if we do
		throw new RuntimeException("User ("+userId+") could NOT delete evaluation ("+evaluationId+")");
	}

	/* (non-Javadoc)
	 * @see edu.vt.sakai.evaluation.logic.EvalEvaluationsLogic#getEvaluationById(java.lang.Long)
	 */
	public EvalEvaluation getEvaluationById(Long evaluationId) {
		log.debug("evalId: " + evaluationId);
		EvalEvaluation togo = (EvalEvaluation) dao.findById(EvalEvaluation.class, evaluationId);
        return wrapEvaluationProxy(togo);
	}

	private EvalEvaluation wrapEvaluationProxy(EvalEvaluation togo) {
      if (togo != null && togo.getId() != null) {
        ProxyFactoryBean pfb = new ProxyFactoryBean();
        pfb.setProxyTargetClass(true);
        pfb.setTargetSource(new SingletonTargetSource(togo));
        pfb.addAdvice(new EvaluationInterceptor(this));
        return (EvalEvaluation) pfb.getObject();
      }
      else return togo;
  }


  /* (non-Javadoc)
	 * @see edu.vt.sakai.evaluation.logic.EvalEvaluationsLogic#getEvaluationsByTemplateId(java.lang.Long)
	 */
	public List getEvaluationsByTemplateId(Long templateId) {
		log.debug("templateId: " + templateId);
		EvalTemplate template = (EvalTemplate) dao.findById(EvalTemplate.class, templateId);
		if (template == null) {
			throw new IllegalArgumentException("Cannot find template with id: " + templateId);
		}
		return dao.findByProperties(EvalEvaluation.class,
				new String[] {"template.id"},  new Object[] {templateId});
	}

	/* (non-Javadoc)
	 * @see edu.vt.sakai.evaluation.logic.EvalEvaluationsLogic#countEvaluationsByTemplateId(java.lang.Long)
	 */
	public int countEvaluationsByTemplateId(Long templateId) {
		log.debug("templateId: " + templateId);
		EvalTemplate template = (EvalTemplate) dao.findById(EvalTemplate.class, templateId);
		if (template == null) {
			throw new IllegalArgumentException("Cannot find template with id: " + templateId);
		}
		return dao.countByProperties(EvalEvaluation.class,
				new String[] {"template.id"},  new Object[] {templateId});
	}

	/* (non-Javadoc)
	 * @see edu.vt.sakai.evaluation.logic.EvalEvaluationsLogic#getVisibleEvaluationsForUser(java.lang.String, boolean)
	 */
	public List getVisibleEvaluationsForUser(String userId, boolean recentOnly) {
		List l = new ArrayList();
		if (recentOnly) {
			// only get recently closed evals 
			// check system setting to get "recent" value
			Integer recentlyClosedDays = (Integer) settings.get(EvalSettings.EVAL_RECENTLY_CLOSED_DAYS);
			Calendar calendar = GregorianCalendar.getInstance();
			calendar.add(Calendar.DATE, -1 * recentlyClosedDays.intValue());
			Date recent = calendar.getTime();

			if (external.isUserAdmin(userId)) {
				l = dao.findByProperties(EvalEvaluation.class,
						new String[] {"stopDate"}, 
						new Object[] {recent}, 
						new int[] {EvaluationDao.GREATER});
			} else {
				l = dao.findByProperties(EvalEvaluation.class,
						new String[] {"owner", "stopDate"}, 
						new Object[] {userId, recent}, 
						new int[] {EvaluationDao.EQUALS, EvaluationDao.GREATER});
			}
		} else {
			// don't worry about when they closed
			if (external.isUserAdmin(userId)) {
				// NOTE: this will probably be too slow -AZ
				l = dao.findAll(EvalEvaluation.class);
			} else {
				// get all evaluations created (owned) by this user
				l = dao.findByProperties(EvalEvaluation.class,
						new String[] {"owner"}, new Object[] {userId});
			}
		}
		return l;
	}

	/* (non-Javadoc)
	 * @see edu.vt.sakai.evaluation.logic.EvalEvaluationsLogic#getEvaluationsForUser(java.lang.String, boolean, boolean)
	 */
	public List getEvaluationsForUser(String userId, boolean activeOnly, boolean untakenOnly) {
		List takeContexts = external.getContextsForUser(userId, EvalConstants.PERM_TAKE_EVALUATION);

		String[] contexts = new String[takeContexts.size()];
		for (int i=0; i<takeContexts.size(); i++) {
			Context c = (Context) takeContexts.get(i);
			contexts[i] = c.context;
		}

		// get the evaluations
		Set s = dao.getEvaluationsByContexts( contexts, activeOnly );

		if (untakenOnly) {
			// filter out the evaluations this user already took

			// create an array of the evaluation ids
			Long[] evalIds = new Long[s.size()];
			int j = 0;
			for (Iterator it = s.iterator(); it.hasNext(); j++) {
				EvalEvaluation eval = (EvalEvaluation) it.next();
				evalIds[j] = (Long) eval.getId();
			}
	
			// now get the responses for all the returned evals
			List l = dao.findByProperties(EvalResponse.class, 
					new String[] {"owner", "evaluation.id"}, 
					new Object[] {userId, evalIds});
	
			// Iterate through and remove the evals this user already took
			for (int i = 0; i < l.size(); i++) {
				EvalResponse er = (EvalResponse) l.get(i);
				s.remove( er.getEvaluation() );
			}
		}

		// stuff the remaining set into a list
		return new ArrayList(s);
	}

	/* (non-Javadoc)
	 * @see edu.vt.sakai.evaluation.logic.EvalEvaluationsLogic#countEvaluationContexts(java.lang.Long)
	 */
	public int countEvaluationContexts(Long evaluationId) {
		log.debug("evalId: " + evaluationId);
		return dao.countByProperties(EvalAssignContext.class, 
				new String[] {"evaluation.id"}, 
				new Object[] {evaluationId});
	}

	/* (non-Javadoc)
	 * @see edu.vt.sakai.evaluation.logic.EvalEvaluationsLogic#getEvaluationContexts(java.lang.Long[])
	 */
	public Map getEvaluationContexts(Long[] evaluationIds) {
		log.debug("evalIds: " + evaluationIds);
		Map evals = new TreeMap();

		// create the inner lists
		for (int i=0; i<evaluationIds.length; i++) {
			List innerList = new ArrayList();
			evals.put(evaluationIds[i], innerList);
		}

		// get all the contexts for the given eval ids in one storage call
		List l = dao.findByProperties(EvalAssignContext.class,
				new String[] {"evaluation.id"},  new Object[] {evaluationIds});
		for (int i=0; i<l.size(); i++) {
			EvalAssignContext eac = (EvalAssignContext) l.get(i);
			String context = eac.getContext();

			// put stuff in inner list
			Long evalId = eac.getEvaluation().getId();
			List innerList = (List) evals.get(evalId);
			innerList.add( external.makeContextObject(context) );
		}
		return evals;
	}

	// PERMISSIONS

	/* (non-Javadoc)
	 * @see edu.vt.sakai.evaluation.logic.EvalEvaluationsLogic#canBeginEvaluation(java.lang.String)
	 */
	public boolean canBeginEvaluation(String userId) {
		log.debug("Checking begin eval for: " + userId);
		boolean isAdmin = external.isUserAdmin(userId);
		if ( isAdmin && (dao.countAll(EvalTemplate.class) > 0) ) {
			// admin can access all templates and create an evaluation if 
			// there is at least one template
			return true;
		}
		if ( external.countContextsForUser(userId, EvalConstants.PERM_ASSIGN_EVALUATION) > 0 ) {
			log.debug("User has permission to assign evaluation in at least one site");

			/*
			 * TODO - Hierarchy
			 * visible and shared sharing methods are meant to work by relating the hierarchy level of 
			 * the owner with the sharing setting in the template, however, that was when 
			 * we assumed there would only be one level per user. That is no longer anything 
			 * we have control over (since we depend on data that comes from another API) 
			 * so we will have to add in a table which will track the hierarchy levels and
			 * link them to the template. This will be a very simple but necessary table.
			 */

			/*
			 * If the person is not an admin (super or any kind, currently we just have super admin) 
			 * then system settings should be checked whether they can create templates 
			 * or not. This is because if they cannot create templates then they cannot start 
			 * evaluations also - kahuja.
			 * 
			 * TODO - this check needs to be more robust at some point
			 * currently we are ignoring shared and visible templates - aaronz.
			 */
			if ( ((Boolean)settings.get(EvalSettings.INSTRUCTOR_ALLOWED_CREATE_EVALUATIONS)).booleanValue() && 
					dao.countVisibleTemplates(userId, new String[] {EvalConstants.SHARING_PUBLIC}, false) > 0 ) {
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see edu.vt.sakai.evaluation.logic.EvalEvaluationsLogic#getEvaluationState(java.lang.Long)
	 */
	public String getEvaluationState(Long evaluationId) {
		log.debug("evalId: " + evaluationId);
		// get evaluation
		EvalEvaluation eval = (EvalEvaluation) dao.findById(EvalEvaluation.class, evaluationId);
		if (eval == null) {
			throw new IllegalArgumentException("Cannot find evaluation with id: " + evaluationId);
		}

		// fix the state of this eval if needed, save it, and return the state constant 
		return fixReturnEvalState(eval, true);
	}

	/* (non-Javadoc)
	 * @see edu.vt.sakai.evaluation.logic.EvalEvaluationsLogic#canRemoveEvaluation(java.lang.String, java.lang.Long)
	 */
	public boolean canRemoveEvaluation(String userId, Long evaluationId) {
		log.debug("evalId: " + evaluationId + ",userId: " + userId);
		EvalEvaluation eval = (EvalEvaluation) dao.findById(EvalEvaluation.class, evaluationId);
		if (eval == null) {
			throw new IllegalArgumentException("Cannot find evaluation with id: " + evaluationId);
		}

		try {
			return canUserRemoveEval(userId, eval);
		} catch (RuntimeException e) {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see edu.vt.sakai.evaluation.logic.EvalEvaluationsLogic#canTakeEvaluation(java.lang.String, java.lang.Long, java.lang.String)
	 */
	public boolean canTakeEvaluation(String userId, Long evaluationId, String context) {
		log.debug("evalId: " + evaluationId + ", userId: " + userId + ", context: " + context);

		// grab the evaluation itself first
		EvalEvaluation eval = (EvalEvaluation) dao.findById(EvalEvaluation.class, evaluationId);
		if (eval == null) {
			throw new IllegalArgumentException("Cannot find evaluation with id: " + evaluationId);
		}

		// check the evaluation state
		String state = EvalUtils.getEvaluationState(eval);
		if ( ! EvalConstants.EVALUATION_STATE_ACTIVE.equals(state) &&
				! EvalConstants.EVALUATION_STATE_DUE.equals(state) ) {
			log.info("User (" + userId + ") cannot take evaluation (" + evaluationId + ") when eval state is: " + state);
			return false;
		}

		// check the user permissions
		if ( ! external.isUserAdmin(userId) && 
				! external.isUserAllowedInContext(userId, 
							EvalConstants.PERM_TAKE_EVALUATION, context) ) {
			log.info("User (" + userId + ") cannot take evaluation (" + evaluationId + ") without permission");
			return false;
		}

		// check that the context is valid for this evaluation
		int evalAssignedContexts = dao.countByProperties(EvalAssignContext.class, 
				new String[] {"evaluation.id", "context"}, 
				new Object[] {evaluationId, context});
		if (evalAssignedContexts <= 0) {
			log.info("User (" + userId + ") cannot take evaluation (" + evaluationId + ") in this context (" + context + "), not assigned");
			return false;			
		}

		// check if the user already took this evaluation
		int evalResponsesForUser = dao.countByProperties(EvalResponse.class, 
				new String[] {"owner", "evaluation.id", "context"}, 
				new Object[] {userId, evaluationId, context});
		if (evalResponsesForUser > 0) {
			// check if persistent object is the one that already exists
			List l = dao.findByProperties(EvalResponse.class, 
					new String[] {"owner", "evaluation.id", "context"}, 
					new Object[] {userId, evaluationId, context});
			EvalResponse response = (EvalResponse) l.get(0);
			if (response.getId() == null && l.size() == 1) {
				// all is ok, the "existing" response is a hibernate persistent object
				// WARNING: this is a bit of a hack though
			} else {
				// user already has a response saved for this evaluation and context
				if (eval.getModifyResponsesAllowed() == null || 
						eval.getModifyResponsesAllowed().booleanValue() == false) {
					// user cannot modify existing responses
					log.info("User (" + userId + ") cannot take evaluation (" + evaluationId + ") again, already taken");
					return false;
				}
			}
		}			


		return true;
	}


	// INTERNAL METHODS

	/**
	 * Check if a user can control an evaluation
	 * @param userId
	 * @param eval
	 * @return true if they can, false otherwise
	 */
	protected boolean canUserControlEvaluation(String userId, EvalEvaluation eval) {
		if ( external.isUserAdmin(userId) ) {
			return true;
		} else if ( eval.getOwner().equals(userId) ) {
			return true;
		}
		return false;
	}

	/**
	 * Check if user can remove this evaluation
	 * @param userId
	 * @param eval
	 * @return truse if they can, false otherwise
	 */
	protected boolean canUserRemoveEval(String userId, EvalEvaluation eval) {
		// if eval id is invalid then just log it
		if (eval == null) {
			log.warn("Cannot find evaluation to delete");
			return false;
		}

		// check user control permissions
		if (! canUserControlEvaluation(userId, eval) ) {
			log.warn("User ("+userId+") attempted to remove evaluation ("+eval.getId()+") without permissions");
			throw new SecurityException("User ("+userId+") attempted to remove evaluation ("+eval.getId()+") without permissions");
		}

		// cannot remove evaluations unless they are still in queue
		// (might adjust this to allow removal if no responses later)
		if ( ! EvalConstants.EVALUATION_STATE_INQUEUE.equals(EvalUtils.getEvaluationState(eval)) ) {
			log.warn("Cannot remove an evaluation ("+eval.getId()+") which has started");
			throw new IllegalStateException("Cannot remove an evaluation ("+eval.getId()+") which has started");
		}

		return true;
	}

	/**
	 * Fixes the state of an evaluation (if needed) and saves it,
	 * will not save state for a new evaluation
	 * 
	 * @param eval
	 * @param saveState if true, save the fixed eval state, else do not save
	 * @return the state constant string
	 */
	protected String fixReturnEvalState(EvalEvaluation eval, boolean saveState) {
		String state = EvalUtils.getEvaluationState(eval);
		// check state against stored state
		if (EvalConstants.EVALUATION_STATE_UNKNOWN.equals(state)) {
			log.warn("Evaluation ("+eval.getTitle()+") in UNKNOWN state");
		} else {
			// compare state and set if not equal
			if (! state.equals(eval.getState()) ) {
				eval.setState(state);
				if ( (eval.getId() != null) && saveState) {
					dao.save(eval);
				}
			}
		}
		return state;
	}

}
