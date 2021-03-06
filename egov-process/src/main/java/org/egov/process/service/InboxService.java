package org.egov.process.service;

import org.activiti.engine.*;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Task;
import org.egov.process.entity.Inbox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class InboxService {

	@Autowired
	private ProcessEngine processEngine;
	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private TaskService taskService;
	@Autowired
	private IdentityService iservice;

	public List<Inbox> search(String userName) {
		List<Inbox> items = new ArrayList<Inbox>();
		try {
			SimpleDateFormat ddmmyyy = new SimpleDateFormat("dd/MM/yyyy");
			Inbox item;
			List<Task> tasks = taskService.createTaskQuery().taskAssignee(userName).list();
			tasks.addAll(taskService.createTaskQuery().taskOwner(userName).list());
			for (Task t : tasks) {

				item = new Inbox();
				item.setTaskId(t.getId());
				item.setSender(getSender(t));
				item.setWfDate(ddmmyyy.format(t.getCreateTime()));
				item.setNatureOfWork(t.getName());
				
				item.setDetails(getDescription(t));
				item.setId(getObjectId(t));;

				item.setLink(getLink(t,item.getId()));
				items.add(item);

			}
			item = new Inbox();
			item.setSender("Sample Sender");
			item.setNatureOfWork("Sample Work");
			item.setLink("Sample Link");
			item.setDetails("Sample details");
			items.add(item);
		} catch (Exception e) {
			
			e.printStackTrace();
		}

		return items;

	}

	private Long getObjectId(Task t) {
        Long id=null;
		Map<String, Object> variables = runtimeService.getVariables(t.getProcessInstanceId());

		if(variables!=null && variables.get("objectId")!=null)
		{
			 Object object = variables.get("objectId");
			 try{
				id= Long.valueOf(object.toString());
			 }catch(Exception e)
			 {

			 }



		}
		return id;


	}

	private String getDescription(Task t) {
		 HistoricTaskInstance previousTask = getPreviousTask(t);
		 HistoryService historyService = processEngine.getHistoryService();
		 List<HistoricVariableInstance> list = historyService.createHistoricVariableInstanceQuery().taskId(t.getId()).list();
		 for(HistoricVariableInstance hvi:list)
		 {
			 System.err.println(hvi.getVariableName().equals("desciption"));
			 hvi.getValue();
		 }
		 return "";
	}

	private String getLink(Task t,Long objectId) {
		String link="";
		if (t.getCategory() != null)
			link= t.getCategory();
		else {
			ProcessDefinition processDefinition = processEngine.getRepositoryService().createProcessDefinitionQuery()
					.processDefinitionId(t.getProcessDefinitionId()).singleResult();
			link= processDefinition.getCategory();
		}
		if(link!=null && !link.isEmpty())
		{
			if(link.indexOf(":objectId")!=-1)
			link=link.replace(":objectId",objectId.toString());
			if(link.indexOf(":taskId")!=-1)
			link=link.replace(":taskId",t.getId());
		}
		return link;

	}

	private String getSender(Task t) {
		return getPreviousTask(t).getAssignee();

		}

	private HistoricTaskInstance getPreviousTask(Task t) {
		HistoryService historyService = processEngine.getHistoryService();
					List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery()
					.processInstanceId(t.getProcessInstanceId()).orderByHistoricTaskInstanceEndTime().desc().list();
			if (list.size() >= 2) {
				return list.get(1);
				
			} else
				return list.get(0);
	}
}