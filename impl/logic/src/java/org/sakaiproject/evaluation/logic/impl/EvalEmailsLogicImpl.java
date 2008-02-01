/**
 * $Id: EvalEmailsLogicImpl.java 1000 Dec 28, 2006 10:07:31 AM azeckoski $
 * $URL: https://source.sakaiproject.org/contrib $
 * EvalEmailsLogicImpl.java - evaluation - Dec 29, 2006 10:07:31 AM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Centre for Applied Research in Educational Technologies, University of Cambridge
 * Licensed under the Educational Community License version 1.0
 * 
 * A copy of the Educational Community License has been included in this 
 * distribution and is available at: http://www.opensource.org/licenses/ecl1.php
 *
 * Aaron Zeckoski (azeckoski@gmail.com) (aaronz@vt.edu) (aaron@caret.cam.ac.uk)
 */

package org.sakaiproject.evaluation.logic.impl;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.evaluation.logic.EvalEmailsLogic;
import org.sakaiproject.evaluation.logic.EvalEvaluationService;
import org.sakaiproject.evaluation.logic.EvalExternalLogic;
import org.sakaiproject.evaluation.logic.EvalSettings;
import org.sakaiproject.evaluation.logic.impl.utils.TextTemplateLogicUtils;
import org.sakaiproject.evaluation.logic.model.EvalGroup;
import org.sakaiproject.evaluation.model.EvalAssignGroup;
import org.sakaiproject.evaluation.model.EvalEmailTemplate;
import org.sakaiproject.evaluation.model.EvalEvaluation;
import org.sakaiproject.evaluation.model.constant.EvalConstants;

/**
 * EvalEmailsLogic implementation,
 * this is a BACKUP service and should only depend on LOWER and BOTTOM services
 * (and maybe other BACKUP services if necessary)
 *
 * @author Aaron Zeckoski (aaronz@vt.edu)
 */
public class EvalEmailsLogicImpl implements EvalEmailsLogic {

   private static Log log = LogFactory.getLog(EvalEmailsLogicImpl.class);

   // Event names cannot be over 32 chars long              // max-32:12345678901234567890123456789012
   protected final String EVENT_EMAIL_CREATED =                      "eval.email.eval.created";
   protected final String EVENT_EMAIL_AVAILABLE =                    "eval.email.eval.available";
   protected final String EVENT_EMAIL_GROUP_AVAILABLE =              "eval.email.evalgroup.available";
   protected final String EVENT_EMAIL_REMINDER =                     "eval.email.eval.reminders";
   protected final String EVENT_EMAIL_RESULTS =                      "eval.email.eval.results";

   private EvalExternalLogic externalLogic;
   public void setExternalLogic(EvalExternalLogic externalLogic) {
      this.externalLogic = externalLogic;
   }

   private EvalSettings settings;
   public void setSettings(EvalSettings settings) {
      this.settings = settings;
   }

   private EvalEvaluationService evaluationService;
   public void setEvaluationService(EvalEvaluationService evaluationService) {
      this.evaluationService = evaluationService;
   }


   // INIT method
   public void init() {
      log.debug("Init");
   }


   public String[] sendEvalCreatedNotifications(Long evaluationId, boolean includeOwner) {
      log.debug("evaluationId: " + evaluationId + ", includeOwner: " + includeOwner);

      EvalEvaluation eval = getEvaluationOrFail(evaluationId);
      String from = getFromEmailOrFail(eval);
      EvalEmailTemplate emailTemplate = getEmailTemplateOrFail(EvalConstants.EMAIL_TEMPLATE_CREATED, evaluationId);

      // append opt-in, opt-out, and/or add questions messages followed by footer
      StringBuilder sb = new StringBuilder();
      int addItems = ((Integer) settings.get(EvalSettings.ADMIN_ADD_ITEMS_NUMBER)).intValue();
      sb.append(emailTemplate.getMessage());
      if (! eval.getInstructorOpt().equals(EvalConstants.INSTRUCTOR_REQUIRED) || (addItems > 0)) {
         if (eval.getInstructorOpt().equals(EvalConstants.INSTRUCTOR_OPT_IN)) {
            // if eval is opt-in notify instructors that they may opt in
            sb.append(EvalConstants.EMAIL_CREATED_OPT_IN_TEXT);
         } else if (eval.getInstructorOpt().equals(EvalConstants.INSTRUCTOR_OPT_OUT)) {
            // if eval is opt-out notify instructors that they may opt out
            sb.append(EvalConstants.EMAIL_CREATED_OPT_OUT_TEXT);
         }
         if (addItems > 0) {
            // if eval allows instructors to add questions notify instructors they may add questions
            sb.append(EvalConstants.EMAIL_CREATED_ADD_ITEMS_TEXT);
         }
      }
      sb.append(EvalConstants.EMAIL_CREATED_DEFAULT_TEXT_FOOTER);
      String message = sb.toString();

      // get the associated groups for this evaluation
      Map<Long, List<EvalGroup>> evalGroups = evaluationService.getEvaluationGroups(new Long[] { evaluationId }, true);

      // only one possible map key so we can assume evaluationId
      List<EvalGroup> groups = evalGroups.get(evaluationId);
      if (log.isDebugEnabled()) {
         log.debug("Found " + groups.size() + " groups for new evaluation: " + evaluationId);
      }

      List<String> sentEmails = new ArrayList<String>();
      // loop through contexts and send emails to correct users in each evalGroupId
      for (int i = 0; i < groups.size(); i++) {
         EvalGroup group = (EvalGroup) groups.get(i);
         if (EvalConstants.GROUP_TYPE_INVALID.equals(group.type)) {
            continue; // skip processing for invalid groups
         }

         Set<String> userIdsSet = externalLogic.getUserIdsForEvalGroup(group.evalGroupId,
               EvalConstants.PERM_BE_EVALUATED);
         // add in the owner or remove them based on the setting
         if (includeOwner) {
            userIdsSet.add(eval.getOwner());
         } else {
            if (userIdsSet.contains(eval.getOwner())) {
               userIdsSet.remove(eval.getOwner());
            }
         }

         // skip ahead if there is no one to send to
         if (userIdsSet.size() == 0) continue;

         // turn the set into an array
         String[] toUserIds = (String[]) userIdsSet.toArray(new String[] {});
         if (log.isDebugEnabled()) {
            log.debug("Found " + toUserIds.length + " users (" + toUserIds + ") to send "
                  + EvalConstants.EMAIL_TEMPLATE_CREATED + " notification to for new evaluation ("
                  + evaluationId + ") and evalGroupId (" + group.evalGroupId + ")");
         }

         // replace the text of the template with real values
         Map<String, String> replacementValues = new HashMap<String, String>();
         replacementValues.put("HelpdeskEmail", from);
         message = makeEmailMessage(message, eval, group, replacementValues);
         String subject = makeEmailMessage(emailTemplate.getSubject(), eval, group, replacementValues);

         // send the actual emails for this evalGroupId
         String[] emailAddresses = externalLogic.sendEmailsToUsers(from, toUserIds, subject, message, true);
         log.info("Sent evaluation created message to " + emailAddresses.length + " users (attempted to send to "+toUserIds.length+")");
         // store sent emails to return
         for (int j = 0; j < emailAddresses.length; j++) {
            sentEmails.add(emailAddresses[j]);            
         }
         externalLogic.registerEntityEvent(EVENT_EMAIL_CREATED, eval);
      }

      return (String[]) sentEmails.toArray(new String[] {});
   }


   public String[] sendEvalAvailableNotifications(Long evaluationId, boolean includeEvaluatees) {
      log.debug("evaluationId: " + evaluationId + ", includeEvaluatees: " + includeEvaluatees);

      Set<String> userIdsSet = null;
      String message = null;
      boolean studentNotification = true;

      // get evaluation
      EvalEvaluation eval = getEvaluationOrFail(evaluationId);
      String from = getFromEmailOrFail(eval);
      EvalEmailTemplate emailTemplate = getEmailTemplateOrFail(EvalConstants.EMAIL_TEMPLATE_AVAILABLE, evaluationId);

      // get the instructor opt-in email template
      EvalEmailTemplate emailOptInTemplate = null;
      if (eval.getInstructorOpt().equals(EvalConstants.INSTRUCTOR_OPT_IN)) {
         emailOptInTemplate = getEmailTemplateOrFail(EvalConstants.EMAIL_TEMPLATE_AVAILABLE_OPT_IN, null);
      }

      // get the associated assign groups for this evaluation
      Map<Long, List<EvalAssignGroup>> evalAssignGroups = 
         evaluationService.getEvaluationAssignGroups(new Long[] { evaluationId }, true);
      List<EvalAssignGroup> assignGroups = evalAssignGroups.get(evaluationId);

      List<String> sentEmails = new ArrayList<String>();
      // loop through groups and send emails to correct users group
      for (int i = 0; i < assignGroups.size(); i++) {
         EvalAssignGroup assignGroup = assignGroups.get(i);
         EvalGroup group = externalLogic.makeEvalGroupObject(assignGroup.getEvalGroupId());
         if (eval.getInstructorOpt().equals(EvalConstants.INSTRUCTOR_REQUIRED)) {
            //notify students
            userIdsSet = externalLogic.getUserIdsForEvalGroup(group.evalGroupId,
                  EvalConstants.PERM_TAKE_EVALUATION);
            studentNotification = true;
         } else {
            //instructor may opt-in or opt-out
            if (assignGroup.getInstructorApproval().booleanValue()) {
               //instructor has opted-in, notify students
               userIdsSet = externalLogic.getUserIdsForEvalGroup(group.evalGroupId,
                     EvalConstants.PERM_TAKE_EVALUATION);
               studentNotification = true;
            } else {
               if (eval.getInstructorOpt().equals(EvalConstants.INSTRUCTOR_OPT_IN) && includeEvaluatees) {
                  // instructor has not opted-in, notify instructors
                  userIdsSet = externalLogic.getUserIdsForEvalGroup(group.evalGroupId,
                        EvalConstants.PERM_BE_EVALUATED);
                  studentNotification = false;
               } else {
                  userIdsSet = new HashSet<String>();
               }
            }
         }

         // skip ahead if there is no one to send to
         if (userIdsSet.size() == 0) continue;

         // turn the set into an array
         String[] toUserIds = (String[]) userIdsSet.toArray(new String[] {});

         if (log.isDebugEnabled()) {
            log.debug("Found " + toUserIds.length + " users (" + toUserIds + ") to send "
                  + EvalConstants.EMAIL_TEMPLATE_CREATED + " notification to for available evaluation ("
                  + evaluationId + ") and group (" + group.evalGroupId + ")");
         }

         // replace the text of the template with real values
         Map<String, String> replacementValues = new HashMap<String, String>();
         replacementValues.put("HelpdeskEmail", from);

         // choose from 2 templates
         EvalEmailTemplate currentTemplate = emailTemplate;
         if (! studentNotification) {
            currentTemplate = emailOptInTemplate;
         }
         message = makeEmailMessage(currentTemplate.getMessage(), eval, group, replacementValues);
         String subject = makeEmailMessage(currentTemplate.getSubject(), eval, group, replacementValues);

         // send the actual emails for this evalGroupId
         String[] emailAddresses = externalLogic.sendEmailsToUsers(from, toUserIds, subject, message, true);
         log.info("Sent evaluation available message to " + emailAddresses.length + " users (attempted to send to "+toUserIds.length+")");
         // store sent emails to return
         for (int j = 0; j < emailAddresses.length; j++) {
            sentEmails.add(emailAddresses[j]);            
         }
         externalLogic.registerEntityEvent(EVENT_EMAIL_AVAILABLE, eval);
      }

      return (String[]) sentEmails.toArray(new String[] {});
   }


   public String[] sendEvalAvailableGroupNotification(Long evaluationId, String evalGroupId) {

      if (evaluationId == null) {
         throw new IllegalStateException("EvalEvaluation id parameter cannot be null");
      }
      if (evalGroupId == null) {
         throw new IllegalStateException("EvalGroup id cannot be null");
      }

      // get evaluation
      EvalEvaluation eval = getEvaluationOrFail(evaluationId);

      List<String> sentMessages = new ArrayList<String>();

      // get group
      EvalGroup group = externalLogic.makeEvalGroupObject(evalGroupId);

      // only process valid groups
      if (!EvalConstants.GROUP_TYPE_INVALID.equals(group.type)) {

         // get the student email template
         EvalEmailTemplate emailTemplate = evaluationService.getEmailTemplate(evaluationId,
               EvalConstants.EMAIL_TEMPLATE_AVAILABLE);
         if (emailTemplate == null) {
            throw new IllegalStateException("Cannot find email template: "
                  + EvalConstants.EMAIL_TEMPLATE_AVAILABLE);
         }
         String from = (String) settings.get(EvalSettings.FROM_EMAIL_ADDRESS);

         //get student ids
         Set<String> userIdsSet = externalLogic.getUserIdsForEvalGroup(group.evalGroupId,
               EvalConstants.PERM_TAKE_EVALUATION);
         if (userIdsSet.size() > 0) {
            String[] toUserIds = (String[]) userIdsSet.toArray(new String[] {});

            // replace the text of the template with real values
            Map<String, String> replacementValues = new HashMap<String, String>();
            replacementValues.put("HelpdeskEmail", from);
            String message = makeEmailMessage(emailTemplate.getMessage(), eval, group, replacementValues);
            String subject = makeEmailMessage(emailTemplate.getSubject(), eval, group, replacementValues);

            // store sent messages to return
            sentMessages.add(message);

            try {
               // send the actual emails for this evalGroupId
               String[] emailAddresses = externalLogic.sendEmailsToUsers(from, toUserIds, subject, message, true);
               log.info("Sent evaluation available group message to " + emailAddresses.length + " users (attempted to send to "+toUserIds.length+")");
               externalLogic.registerEntityEvent(EVENT_EMAIL_GROUP_AVAILABLE, eval);
            } catch (Exception e) {
               log.error(this + ".sendEvalAvailableGroupNotification(" + evaluationId + "," + evalGroupId
                     + ") externalLogic.sendEmails " + e);
            }
         }
      }

      return (String[]) sentMessages.toArray(new String[] {});
   }


   public String[] sendEvalReminderNotifications(Long evaluationId, String includeConstant) {
      log.debug("evaluationId: " + evaluationId + ", includeConstant: " + includeConstant);
      if (includeConstant == null
            || !(includeConstant == EvalConstants.EMAIL_INCLUDE_NONTAKERS || includeConstant == EvalConstants.EMAIL_INCLUDE_ALL)) {
         log.error("includeConstant null or unknown");
         return null;
      }

      String from = (String) settings.get(EvalSettings.FROM_EMAIL_ADDRESS);

      // get evaluation
      EvalEvaluation eval = getEvaluationOrFail(evaluationId);

      // get the email template
      EvalEmailTemplate emailTemplate = evaluationService.getEmailTemplate(evaluationId, EvalConstants.EMAIL_TEMPLATE_REMINDER);

      if (emailTemplate == null) {
         throw new IllegalStateException("Cannot find email template: "
               + EvalConstants.EMAIL_TEMPLATE_REMINDER);
      }

      List<String> sentMessages = new ArrayList<String>();

      // get the associated eval groups for this evaluation
      // NOTE: this only returns the groups that should get emails, there is no need to do an additional check
      // to see if the instructor has opted in in this case -AZ
      Map<Long, List<EvalGroup>> evalGroupIds = evaluationService.getEvaluationGroups(new Long[] { evaluationId }, false);

      // only one possible map key so we can assume evaluationId
      List<EvalGroup> groups = evalGroupIds.get(evaluationId);
      log.debug("Found " + groups.size() + " groups for available evaluation: " + evaluationId);

      Set<String> userIdsSet = new HashSet<String>();

      // loop through groups and send emails to correct users in each
      for (int i = 0; i < groups.size(); i++) {
         EvalGroup group = (EvalGroup) groups.get(i);
         if (EvalConstants.GROUP_TYPE_INVALID.equals(group.type)) {
            continue; // skip processing for invalid groups
         }

         userIdsSet.clear();
         if (EvalConstants.EMAIL_INCLUDE_NONTAKERS.equals(includeConstant)) {
            userIdsSet.addAll(getNonResponders(evaluationId, group.evalGroupId));
         } else if (EvalConstants.EMAIL_INCLUDE_ALL.equals(includeConstant)) {
            userIdsSet.addAll(getNonResponders(evaluationId, group.evalGroupId));
            userIdsSet.addAll(externalLogic.getUserIdsForEvalGroup(group.evalGroupId,
                  EvalConstants.PERM_BE_EVALUATED));
         }

         if (userIdsSet.size() > 0) {
            // turn the set into an array
            String[] toUserIds = (String[]) userIdsSet.toArray(new String[] {});
            log.debug("Found " + toUserIds.length + " users (" + toUserIds + ") to send "
                  + EvalConstants.EMAIL_TEMPLATE_REMINDER + " notification to for available evaluation ("
                  + evaluationId + ") and group (" + group.evalGroupId + ")");

            // replace the text of the template with real values
            Map<String, String> replacementValues = new HashMap<String, String>();
            replacementValues.put("HelpdeskEmail", from);
            String message = makeEmailMessage(emailTemplate.getMessage(), eval, group, replacementValues);
            String subject = makeEmailMessage(emailTemplate.getSubject(), eval, group, replacementValues);

            // store sent messages to return
            sentMessages.add(message);

            // send the actual emails for this evalGroupId
            try {
               String[] emailAddresses = externalLogic.sendEmailsToUsers(from, toUserIds, subject, message, true);
               log.info("Sent evaluation reminder message to " + emailAddresses.length + " users (attempted to send to "+toUserIds.length+")");
               externalLogic.registerEntityEvent(EVENT_EMAIL_REMINDER, eval);
            } catch (Exception e) {
               log.error(this + ".sendEvalReminderNotifications(" + evaluationId + "," + includeConstant
                     + ") externalLogic.sendEmails " + e);
            }
         }
      }

      return (String[]) sentMessages.toArray(new String[] {});
   }


   public String[] sendEvalResultsNotifications(Long evaluationId, boolean includeEvaluatees,
         boolean includeAdmins, String jobType) {
      log.debug("evaluationId: " + evaluationId + ", includeEvaluatees: " + includeEvaluatees
            + ", includeAdmins: " + includeAdmins);
      String from = (String) settings.get(EvalSettings.FROM_EMAIL_ADDRESS);
      EvalAssignGroup evalAssignGroup = null;

      /*TODO deprecated?
       if(EvalConstants.EMAIL_INCLUDE_ALL.equals(includeConstant)) {
       }
       boolean includeEvaluatees = true;
       if (includeEvaluatees) {
       // TODO Not done yet
       log.error("includeEvaluatees Not implemented");
       }
       */

      // get evaluation
      EvalEvaluation eval = getEvaluationOrFail(evaluationId);

      // get the email template
      EvalEmailTemplate emailTemplate = evaluationService.getDefaultEmailTemplate(EvalConstants.EMAIL_TEMPLATE_RESULTS);

      if (emailTemplate == null) {
         throw new IllegalStateException("Cannot find email template: "
               + EvalConstants.EMAIL_TEMPLATE_RESULTS);
      }

      List<String> sentMessages = new ArrayList<String>();

      // get the associated eval groups for this evaluation
      Map<Long, List<EvalGroup>> evalGroupIds = evaluationService.getEvaluationGroups(new Long[] { evaluationId }, false);

      //get the associated eval assign groups for this evaluation
      Map<Long, List<EvalAssignGroup>> evalAssignGroups = evaluationService.getEvaluationAssignGroups(new Long[] { evaluationId }, false);

      // only one possible map key so we can assume evaluationId
      List<EvalGroup> groups = evalGroupIds.get(evaluationId);
      log.debug("Found " + groups.size() + " groups for available evaluation: " + evaluationId);
      List<EvalAssignGroup> assignGroups = evalAssignGroups.get(evaluationId);
      log.debug("Found " + assignGroups.size() + " assign groups for available evaluation: " + evaluationId);

      Set<String> userIdsSet = new HashSet<String>();

      // loop through contexts and send emails to correct users in each evalGroupId
      for (int i = 0; i < groups.size(); i++) {
         EvalGroup group = (EvalGroup) groups.get(i);
         if (EvalConstants.GROUP_TYPE_INVALID.equals(group.type)) {
            continue; // skip processing for invalid groups
         }

         //get EvalAssignGroup to check studentViewResults, instructorViewResults
         for (Iterator<EvalAssignGroup> j = assignGroups.iterator(); j.hasNext();) {
            EvalAssignGroup assignGroup = j.next();
            if (group.evalGroupId.equals(assignGroup.getEvalGroupId())) {
               evalAssignGroup = assignGroup;
            }
         }

         userIdsSet.clear();

         /*
          * Notification of results may occur on separate dates for owner,
          * instructors, and students. Job type is used to distinguish the
          * intended recipient group.
          */

         //always send results email to eval.getOwner()
         if (jobType.equals(EvalConstants.JOB_TYPE_VIEWABLE)) {
            userIdsSet.add(eval.getOwner());
         }

         //if results are not private
         if (!eval.getResultsPrivate().booleanValue()) {

            //at present, includeAdmins is always true
            if (includeAdmins && evalAssignGroup.getInstructorsViewResults().booleanValue()
                  && jobType.equals(EvalConstants.JOB_TYPE_VIEWABLE_INSTRUCTORS)) {
               userIdsSet.addAll(externalLogic.getUserIdsForEvalGroup(group.evalGroupId,
                     EvalConstants.PERM_BE_EVALUATED));
            }

            //at present, includeEvaluatees is always true
            if (includeEvaluatees && evalAssignGroup.getStudentsViewResults().booleanValue()
                  && jobType.equals(EvalConstants.JOB_TYPE_VIEWABLE_STUDENTS)) {
               userIdsSet.addAll(externalLogic.getUserIdsForEvalGroup(group.evalGroupId,
                     EvalConstants.PERM_TAKE_EVALUATION));
            }
         }

         if (userIdsSet.size() > 0) {
            // turn the set into an array
            String[] toUserIds = (String[]) userIdsSet.toArray(new String[] {});
            log.debug("Found " + toUserIds.length + " users (" + toUserIds + ") to send "
                  + EvalConstants.EMAIL_TEMPLATE_RESULTS + " notification to for available evaluation ("
                  + evaluationId + ") and group (" + group.evalGroupId + ")");

            // replace the text of the template with real values
            Map<String, String> replacementValues = new HashMap<String, String>();
            replacementValues.put("HelpdeskEmail", from);
            String message = makeEmailMessage(emailTemplate.getMessage(), eval, group, replacementValues);
            String subject = makeEmailMessage(emailTemplate.getSubject(), eval, group, replacementValues);

            // store sent messages to return
            sentMessages.add(message);

            // send the actual emails for this evalGroupId
            try {
               String[] emailAddresses = externalLogic.sendEmailsToUsers(from, toUserIds, subject, message, true);
               log.info("Sent evaluation results message to " + emailAddresses.length + " users (attempted to send to "+toUserIds.length+")");
               externalLogic.registerEntityEvent(EVENT_EMAIL_RESULTS, eval);
            } catch (Exception e) {
               log.error(this + ".sendEvalResultsNotifications(" + evaluationId + "," + includeEvaluatees
                     + "," + includeAdmins + ") externalLogic.sendEmails " + e);
            }
         }
      }
      return (String[]) sentMessages.toArray(new String[] {});
   }


   public Set<String> getUserEmailsForEvaluation(Long evaluationId, String evalGroupId, String includeConstant, boolean returnUserIds) {
      log.debug("evaluationId: " + evaluationId + ", includeConstant: " + includeConstant);
      if (includeConstant == null
            || !(includeConstant == EvalConstants.EMAIL_INCLUDE_NONTAKERS || includeConstant == EvalConstants.EMAIL_INCLUDE_ALL)) {
         log.error("includeConstant null or unknown");
         return null;
      }

      // check evaluation
      if (! evaluationService.checkEvaluationExists(evaluationId)) {
         throw new IllegalArgumentException("Invalid evaluation id, cannot find evaluation with this id: " + evaluationId);
      }

      // get the associated eval groups for this evaluation
      // NOTE: this only returns the groups that should get emails, there is no need to do an additional check
      // to see if the instructor has opted in in this case -AZ
      Map<Long, List<EvalGroup>> evalGroupIds = evaluationService.getEvaluationGroups(new Long[] { evaluationId }, false);

      // only one possible map key so we can assume evaluationId
      List<EvalGroup> groups = evalGroupIds.get(evaluationId);
      log.debug("Found " + groups.size() + " groups for evaluation: " + evaluationId);

      Set<String> userIdsSet = new HashSet<String>();

      // loop through groups and get users
      for (int i = 0; i < groups.size(); i++) {
         EvalGroup group = (EvalGroup) groups.get(i);
         if (EvalConstants.GROUP_TYPE_INVALID.equals(group.type)) {
            continue; // skip processing for invalid groups
         }

         if (EvalConstants.EMAIL_INCLUDE_NONTAKERS.equals(includeConstant)) {
            userIdsSet.addAll(getNonResponders(evaluationId, group.evalGroupId));
         } else if (EvalConstants.EMAIL_INCLUDE_ALL.equals(includeConstant)) {
            userIdsSet.addAll(getNonResponders(evaluationId, group.evalGroupId));
            userIdsSet.addAll(externalLogic.getUserIdsForEvalGroup(group.evalGroupId,
                  EvalConstants.PERM_BE_EVALUATED));
         }
      }

      return userIdsSet;
   }


   public Set<String> getNonResponders(Long evaluationId, String evalGroupId) {
      Long[] evaluationIds = { evaluationId };
      Set<String> userIds = new HashSet<String>();

      // get everyone permitted to take the evaluation
      Set<String> ids = externalLogic.getUserIdsForEvalGroup(evalGroupId, EvalConstants.PERM_TAKE_EVALUATION);
      for (Iterator<String> i = ids.iterator(); i.hasNext();) {
         String userId = i.next();

         // if this user hasn't submitted a response, add the user's id
         // FIXME This call is BRUTALLY inefficient -AZ
         // TODO FIX THIS!
         if (evaluationService.getEvaluationResponses(userId, evaluationIds, null, true).isEmpty()) {
            userIds.add(userId);
         }
      }
      return userIds;
   }


   // INTERNAL METHODS

   /**
    * INTERNAL METHOD<br/>
    * Builds the email message from a template and a bunch of variables
    * (passed in and otherwise)
    * 
    * @param messageTemplate
    * @param eval
    * @param group
    * @param replacementValues a map of String -> String representing $keys in the template to replace with text values
    * @return the processed message template with replacements and logic handled
    */
   protected String makeEmailMessage(String messageTemplate, EvalEvaluation eval, EvalGroup group,
         Map<String, String> replacementValues) {
      // replace the text of the template with real values
      if (replacementValues == null) {
         replacementValues = new HashMap<String, String>();
      }
      replacementValues.put("EvalTitle", eval.getTitle());

      // use a date which is related to the current users locale
      DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, 
            externalLogic.getUserLocale(externalLogic.getCurrentUserId()));

      replacementValues.put("EvalStartDate", df.format(eval.getStartDate()));
      replacementValues.put("EvalDueDate", df.format(eval.getDueDate()));
      replacementValues.put("EvalResultsDate", df.format(eval.getViewDate()));
      replacementValues.put("EvalGroupTitle", group.title);

      // generate URLs to the evaluation
      String evalEntityURL = null;
      if (group != null && group.evalGroupId != null) {
         // get the URL directly to the evaluation with group context included
         Long assignGroupId = evaluationService.getAssignGroupId(eval.getId(), group.evalGroupId);
         EvalAssignGroup assignGroup = evaluationService.getAssignGroupById(assignGroupId);
         if (assignGroup != null) {
            evalEntityURL = externalLogic.getEntityURL(assignGroup);
         }
      }

      if (evalEntityURL == null) {
         // just get the URL to the evaluation without group context
         evalEntityURL = externalLogic.getEntityURL(eval);
      }

      // all URLs are identical because the user permissions determine access uniquely
      replacementValues.put("URLtoTakeEval", evalEntityURL);
      replacementValues.put("URLtoAddItems", evalEntityURL);
      replacementValues.put("URLtoOptIn", evalEntityURL);
      replacementValues.put("URLtoOptOut", evalEntityURL);
      replacementValues.put("URLtoViewResults", evalEntityURL);
      replacementValues.put("URLtoSystem", externalLogic.getServerUrl());

      return TextTemplateLogicUtils.processTextTemplate(messageTemplate, replacementValues);
   }

   /**
    * INTERNAL METHOD<br/>
    * Get an email template by type and evaluationId or fail
    * @param typeConstant an EvalConstants.EMAIL_TEMPLATE constant
    * @param evaluationId unique id of an eval or null to only get the default template
    * @return an email template
    * @throws IllegalStateException if no email template can be found
    */
   protected EvalEmailTemplate getEmailTemplateOrFail(String typeConstant, Long evaluationId) {
      EvalEmailTemplate emailTemplate = evaluationService.getDefaultEmailTemplate(typeConstant);
      if (evaluationId != null &&
            ( EvalConstants.EMAIL_TEMPLATE_AVAILABLE.equals(typeConstant) ||
            EvalConstants.EMAIL_TEMPLATE_REMINDER.equals(typeConstant) ) ) {
         EvalEmailTemplate evalEmailTemplate = evaluationService.getEmailTemplate(evaluationId, typeConstant);
         if (evalEmailTemplate != null) {
            emailTemplate = evalEmailTemplate;
         }
      }
      if (emailTemplate == null) {
         throw new IllegalStateException("Cannot find email template default or in eval ("+evaluationId+"): " + typeConstant);
      }
      return emailTemplate;
   }

   /**
    * INTERNAL METHOD<br/>
    * Get the email address from system settings or the evaluation
    * @param eval
    * @return an email address
    * @throws IllegalStateException if a from address cannot be found
    */
   protected String getFromEmailOrFail(EvalEvaluation eval) {
      String from = (String) settings.get(EvalSettings.FROM_EMAIL_ADDRESS);
      if (eval.getReminderFromEmail() != null && ! "".equals(eval.getReminderFromEmail())) {
         from = eval.getReminderFromEmail();
      }
      if (from == null) {
         throw new IllegalStateException("Could not get a from email address from system settings or the evaluation");
      }
      return from;
   }

   /**
    * INTERNAL METHOD<br/>
    * Gets the evaluation or throws exception,
    * reduce code duplication
    * @param evaluationId
    * @return eval for this id
    * @throws IllegalArgumentException if no eval exists
    */
   protected EvalEvaluation getEvaluationOrFail(Long evaluationId) {
      EvalEvaluation eval = evaluationService.getEvaluationById(evaluationId);
      if (eval == null) {
         throw new IllegalArgumentException("Cannot find evaluation with id: " + evaluationId);
      }
      return eval;
   }

}
