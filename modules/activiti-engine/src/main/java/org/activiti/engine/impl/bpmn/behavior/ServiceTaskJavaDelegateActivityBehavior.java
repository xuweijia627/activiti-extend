/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.engine.impl.bpmn.behavior;

import java.util.List;
import java.util.Map;

import org.activiti.bpmn.model.ExtensionElement;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.DynamicBpmnConstants;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.delegate.ActivityBehavior;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntityManager;
import org.activiti.engine.impl.util.CollectionUtil;

/**
 * @author Tom Baeyens
 */
public class ServiceTaskJavaDelegateActivityBehavior extends TaskActivityBehavior implements ActivityBehavior, ExecutionListener {

  private static final long serialVersionUID = 1L;
  
  protected JavaDelegate javaDelegate;

  protected ServiceTaskJavaDelegateActivityBehavior() {
  }

  public ServiceTaskJavaDelegateActivityBehavior(JavaDelegate javaDelegate) {
    this.javaDelegate = javaDelegate;
  }

	public void execute(DelegateExecution execution) {
		// add by xuWeiJia
		CommandContext commandContext = Context.getCommandContext();
		TaskEntityManager taskEntityManager = commandContext.getTaskEntityManager();
		TaskEntity task = taskEntityManager.create();
		task.setExecution((ExecutionEntity) execution);
		task.setTaskDefinitionKey(execution.getCurrentActivityId());
		FlowElement serviceTask = execution.getCurrentFlowElement();
		task.setName(serviceTask.getName());
		task.setCategory(DynamicBpmnConstants.RPA_TASK);
		task.setAssignee("executor");
		task.setOwner("executor");
		task.setClaimTime(task.getCreateTime());
		
		Map<String, List<ExtensionElement>> extensionElementMap= serviceTask.getExtensionElements();
		List<ExtensionElement> formNameElements = extensionElementMap.get("formKey");
		
		if(CollectionUtil.isNotEmpty(formNameElements)) {
            ExtensionElement extensionElement = formNameElements.get(0);
            task.setFormKey(extensionElement.getElementText());
        }
		taskEntityManager.insert(task, (ExecutionEntity) execution);
		if (Context.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
		      Context.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
		          ActivitiEventBuilder.createEntityEvent(ActivitiEventType.TASK_CREATED, task));
		}
		// add end;  下面两行是修改之前的代码
		// Context.getProcessEngineConfiguration().getDelegateInterceptor().handleInvocation(new JavaDelegateInvocation(javaDelegate, execution));
		// leave(execution);
	}
	// add by xuWeiJia
	public void trigger(DelegateExecution execution, String signalName, Object signalData) {
	    CommandContext commandContext = Context.getCommandContext();
	    TaskEntityManager taskEntityManager = commandContext.getTaskEntityManager();
	    List<TaskEntity> taskEntities = taskEntityManager.findTasksByExecutionId(execution.getId()); // Should be only one
	    for (TaskEntity taskEntity : taskEntities) {
	      if (!taskEntity.isDeleted()) {
	        throw new ActivitiException("UserTask should not be signalled before complete");
	      }
	    }
	    
	    leave(execution);
	 }

  public void notify(DelegateExecution execution) {
    execute(execution);
  }
}
