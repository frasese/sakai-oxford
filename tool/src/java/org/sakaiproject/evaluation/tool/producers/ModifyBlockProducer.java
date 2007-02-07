/******************************************************************************
 * ModifyBlockProducer.java - created by fengr@vt.edu on Oct 2, 2006
 * 
 * Copyright (c) 2007 Virginia Polytechnic Institute and State University
 * Licensed under the Educational Community License version 1.0
 * 
 * A copy of the Educational Community License has been included in this 
 * distribution and is available at: http://www.opensource.org/licenses/ecl1.php
 * 
 * Contributors:
 * Rui Feng (fengr@vt.edu)
 *****************************************************************************/
package org.sakaiproject.evaluation.tool.producers;

import java.util.ArrayList;
import java.util.List;

import org.sakaiproject.evaluation.logic.EvalItemsLogic;
import org.sakaiproject.evaluation.model.EvalTemplate;
import org.sakaiproject.evaluation.model.EvalTemplateItem;
import org.sakaiproject.evaluation.model.constant.EvalConstants;
import org.sakaiproject.evaluation.tool.EvaluationConstant;
import org.sakaiproject.evaluation.tool.TemplateBean;
import org.sakaiproject.evaluation.tool.params.BlockIdsParameters;
import org.sakaiproject.evaluation.tool.params.EvalViewParameters;
import org.sakaiproject.evaluation.tool.utils.ItemBlockUtils;


import uk.org.ponder.messageutil.MessageLocator;
import uk.org.ponder.rsf.components.ELReference;
import uk.org.ponder.rsf.components.UIBoundBoolean;
import uk.org.ponder.rsf.components.UIBoundList;
import uk.org.ponder.rsf.components.UIBranchContainer;
import uk.org.ponder.rsf.components.UICommand;
import uk.org.ponder.rsf.components.UIContainer;
import uk.org.ponder.rsf.components.UIELBinding;
import uk.org.ponder.rsf.components.UIForm;
import uk.org.ponder.rsf.components.UIInput;
import uk.org.ponder.rsf.components.UIInternalLink;
import uk.org.ponder.rsf.components.UIOutput;
import uk.org.ponder.rsf.components.UISelect;
import uk.org.ponder.rsf.components.UISelectChoice;
import uk.org.ponder.rsf.flow.jsfnav.DynamicNavigationCaseReporter;
import uk.org.ponder.rsf.flow.jsfnav.NavigationCase;
import uk.org.ponder.rsf.flow.jsfnav.NavigationCaseReporter;
import uk.org.ponder.rsf.view.ComponentChecker;
import uk.org.ponder.rsf.view.ViewComponentProducer;
import uk.org.ponder.rsf.viewstate.SimpleViewParameters;
import uk.org.ponder.rsf.viewstate.ViewParameters;
import uk.org.ponder.rsf.viewstate.ViewParamsReporter;

/**
 * Page for Create, modify, preview, delete a Block type Item
 * 
 * @author: Rui Feng (fengr@vt.edu)
 */

public class ModifyBlockProducer implements ViewComponentProducer,ViewParamsReporter,DynamicNavigationCaseReporter{
	public static final String VIEW_ID = "modify_block"; //$NON-NLS-1$
	public String getViewID() {
		return VIEW_ID;
	}
	
	private MessageLocator messageLocator;
	public void setMessageLocator(MessageLocator messageLocator) {
		this.messageLocator = messageLocator;
	}	
	
	private EvalItemsLogic itemsLogic;

	public void setItemsLogic(EvalItemsLogic itemsLogic) {
	    this.itemsLogic = itemsLogic;
	}

	// Permissible since is a request-scope producer. Accessed from NavigationCases
	private Long templateId; 

	public void fillComponents(UIContainer tofill, ViewParameters viewparams, ComponentChecker checker) {	
		BlockIdsParameters evParameters = (BlockIdsParameters)viewparams;
		
		if(evParameters != null && evParameters.templateItemIds != null){
			templateId =  evParameters.templateId;
				    
			System.out.println("templateId="+evParameters.templateId);
			System.out.println("block item ids="+evParameters.templateItemIds);
			
			//analyze the string of templateItemIds
			String[] strIds = evParameters.templateItemIds.split(",");
				EvalTemplateItem templateItems[] = new EvalTemplateItem[strIds.length];
			for(int i=0; i< strIds.length;i++){
				System.out.println("checked id["+i+"]="+ strIds[i]);
				templateItems[i]= itemsLogic.getTemplateItemById(Long.valueOf(strIds[i]));
				//TODO:check if each templateItem has the same scale, otherwise throw error 
				System.out.println("TemplateItems["+i+"].itemText="+templateItems[i].getItem().getItemText() );		
			}
			
			//TODO:JAVACRIPT need to enforce at leat 2 items are checked for Block creation on TemplateModify page
			//if strIds length is 1, then this is from an existing Block type--from Modify Link on TemplateModify page
			
			
			//render page
			UIOutput.make(tofill, "modify-block-title", messageLocator.getMessage("modifyblock.page.title")); //$NON-NLS-1$ //$NON-NLS-2$
			UIOutput.make(tofill, "create-eval-title", messageLocator.getMessage("createeval.page.title")); //$NON-NLS-1$ //$NON-NLS-2$
			
			UIInternalLink.make(tofill, "summary-toplink", messageLocator.getMessage("summary.page.title"),  //$NON-NLS-1$ //$NON-NLS-2$
								new SimpleViewParameters(SummaryProducer.VIEW_ID));			
			
			UIForm form = UIForm.make(tofill, "blockForm"); //$NON-NLS-1$
			
			UIOutput.make(form, "item-header", messageLocator.getMessage("modifyitem.item.header")); //$NON-NLS-1$ //$NON-NLS-2$
			UIOutput.make(form,"itemNo","1."); //TODO:
			UIOutput.make(form,"itemClassification",messageLocator.getMessage("modifytemplate.itemtype.block"));
			UIOutput.make(form, "added-by", messageLocator.getMessage("modifyitem.added.by"));
			UIOutput.make(form, "userInfo",templateItems[0].getOwner());
			//TODO: remove link
			
			UIOutput.make(form, "item-header-text-header", messageLocator.getMessage("modifyblock.item.header.text.header"));
			//UIInput.make(form, "item_text", "templateItemBeanLocator.new1."+"item.itemText", null);
			
		    UIOutput.make(form, "scale-type-header", messageLocator.getMessage("modifyblock.scale.type.header")); 
			UIOutput.make(form, "scaleLabel",templateItems[0].getItem().getScale().getTitle());
			
			UIOutput.make(form,"add-na-header",messageLocator.getMessage("modifyitem.add.na.header")); //$NON-NLS-1$ //$NON-NLS-2$
			//UIBoundBoolean.make(form, "item_NA",  "templateItemBeanLocator.new1."+ "usesNA", null); //$NON-NLS-1$ //$NON-NLS-2$
			
			UIOutput.make(form,"ideal-coloring-header",messageLocator.getMessage("modifyblock.ideal.coloring.header")); //$NON-NLS-1$ //$NON-NLS-2$
			//UIBoundBoolean.make(form, "idealColor", "#{templateBBean.idealColor}",null); //$NON-NLS-1$ //$NON-NLS-2$
			
			UIOutput.make(form,"item-category-header",messageLocator.getMessage("modifyitem.item.category.header")); //$NON-NLS-1$ //$NON-NLS-2$
			UIOutput.make(form,"course-category-header",messageLocator.getMessage("modifyitem.course.category.header")); //$NON-NLS-1$ //$NON-NLS-2$
			UIOutput.make(form,"instructor-category-header",messageLocator.getMessage("modifyitem.instructor.category.header")); //$NON-NLS-1$ //$NON-NLS-2$
			//Radio Buttons for "Item Category"
			String[] courseCategoryList = 
			{
				messageLocator.getMessage("modifyitem.course.category.header"),
				messageLocator.getMessage("modifyitem.instructor.category.header"),
			};
			
			UISelect radios = null;			
			if(templateItems.length ==1){
				UIInput.make(form, "item_text", "templateItemBeanLocator."+templateItems[0].getId()+".item.itemText", null);
					UIBoundBoolean.make(form, "item_NA",  "templateItemBeanLocator."+ templateItems[0].getId()+".usesNA", null); 
				if(templateItems[0].getScaleDisplaySetting()!= null && 
						templateItems[0].getScaleDisplaySetting().equals(EvalConstants.ITEM_SCALE_DISPLAY_STEPPED_COLORED))
					UIBoundBoolean.make(form, "idealColor", "#{templateBBean.idealColor}",Boolean.TRUE);
				else UIBoundBoolean.make(form, "idealColor", "#{templateBBean.idealColor}",null);
				radios = UISelect.make(form, "item_category",
			            EvaluationConstant.ITEM_CATEGORY_VALUES, courseCategoryList,
			            "templateItemBeanLocator."+templateItems[0].getId()+".itemCategory", null);
			}else{
				UIInput.make(form, "item_text", "templateItemBeanLocator.new1."+"item.itemText", null);
				UIBoundBoolean.make(form, "item_NA",  "templateItemBeanLocator.new1."+ "usesNA", null); 
				UIBoundBoolean.make(form, "idealColor", "#{templateBBean.idealColor}",null); 
				radios = UISelect.make(form, "item_category",
			            EvaluationConstant.ITEM_CATEGORY_VALUES, courseCategoryList,
			            "templateItemBeanLocator.new1."+"itemCategory", null);
			}
		

			String selectID = radios.getFullID();
			UISelectChoice.make(form, "item_category_C", selectID, 0); //$NON-NLS-1$
			UISelectChoice.make(form, "item_category_I", selectID, 1);
			
			
			if(templateItems.length ==1){//for modify existing block item
				//get Block child item
				EvalTemplate template = templateItems[0].getTemplate();
				List l = itemsLogic.getTemplateItemsForTemplate(template.getId(),null);
				List childList = ItemBlockUtils.getChildItmes(l,new Integer(templateItems[0].getId().intValue()));				
				for(int i= 0; i< childList.size();i++){
					EvalTemplateItem child = (EvalTemplateItem)childList.get(i);
					UIBranchContainer radiobranch = UIBranchContainer.make(form,"queRow:",Integer.toString(i)); //$NON-NLS-1$
					UIOutput.make(radiobranch, "childOrder",Integer.toString(i+1));
					UIInput.make(radiobranch,"queText",null,child.getItem().getItemText() );	
				} 				
			}else{
				//render child textfield form passed Ids for new Block Creation
				for(int i= 0;i< templateItems.length; i++){
					UIBranchContainer radiobranch = UIBranchContainer.make(form,"queRow:",Integer.toString(i)); //$NON-NLS-1$
					UIOutput.make(radiobranch, "childOrder",Integer.toString(i+1));
					UIInput.make(radiobranch,"queText",null,templateItems[i].getItem().getItemText() );		
				}
			}
			
			
			UIOutput.make(form, "cancel-button", messageLocator.getMessage("general.cancel.button"));
	
			UICommand  saveCmd = UICommand.make(form, "saveBlockAction", messageLocator.getMessage("modifyitem.save.button"), 
					"#{templateBBean.saveBlockItemAction}");
			saveCmd.parameters.add(new UIELBinding("#{templateBBean.childTemplateItemIds}",
					evParameters.templateItemIds));
			
		}
	}


	public List reportNavigationCases() {
		List i = new ArrayList();
		
		i.add(new NavigationCase(PreviewItemProducer.VIEW_ID, new SimpleViewParameters(PreviewItemProducer.VIEW_ID)));
		i.add(new NavigationCase("success",
		        new EvalViewParameters(TemplateModifyProducer.VIEW_ID, templateId)));

		return i;
	}

	public ViewParameters getViewParameters() {
		return new BlockIdsParameters();
	}

	
}
