package org.activiti.engine.impl.cmd;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.List;

import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.deploy.DeploymentManager;
import org.activiti.engine.impl.persistence.entity.DeploymentEntity;
import org.activiti.engine.impl.persistence.entity.ResourceEntity;
import org.activiti.engine.impl.persistence.entity.ResourceEntityManager;
import org.activiti.engine.impl.repository.DeploymentBuilderImpl;
import org.activiti.engine.impl.util.IoUtil;
import org.activiti.engine.repository.Deployment;
/**
 * 扩展的部署命令，用于不发版本 修改流程定义
 * @author xuWeiJia
 */
public class DeployExtensionCmd<T> implements Command<Deployment>, Serializable {

	private static final long serialVersionUID = 1L;
	protected DeploymentBuilderImpl deploymentBuilder;
	private BpmnModel bpmnModel;
	private String deploymentId;
	protected BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();

	public DeployExtensionCmd(BpmnModel bpmnModel, String deploymentId, DeploymentBuilderImpl deploymentBuilder) {
		this.deploymentBuilder = deploymentBuilder;
		this.bpmnModel = bpmnModel;
		this.deploymentId = deploymentId;
	}
	
	@Override
	public Deployment execute(CommandContext commandContext) {
		ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
		ResourceEntityManager resourceEntityManager = processEngineConfiguration.getResourceEntityManager();
		
		byte[] bpmnBytes = IoUtil.readInputStream(new ByteArrayInputStream(bpmnXMLConverter.convertToXML(bpmnModel)), null);
		
		byte[] diagramBytes = IoUtil.readInputStream(
		          processEngineConfiguration.getProcessDiagramGenerator().generateDiagram(bpmnModel, "png",
		              processEngineConfiguration.getActivityFontName(),
		              processEngineConfiguration.getLabelFontName(),
		              processEngineConfiguration.getAnnotationFontName(),
		              processEngineConfiguration.getClassLoader()), null);
		
		List<ResourceEntity> resourceList= resourceEntityManager.findResourcesByDeploymentId(deploymentId);
		for (ResourceEntity resourceEntity : resourceList) {
			ResourceEntity newDeployEntity = resourceEntityManager.create();
			newDeployEntity.setId(resourceEntity.getId());
			newDeployEntity.setUpdated(true);
			if(resourceEntity.getName().endsWith(".png")) {
				newDeployEntity.setBytes(diagramBytes);
			} else if(resourceEntity.getName().endsWith(".bpmn")) {
				newDeployEntity.setBytes(bpmnBytes);
			}
			resourceEntityManager.update(newDeployEntity, false);
		}
		
		DeploymentManager deploymentCache = processEngineConfiguration.getDeploymentManager();
		deploymentCache.getProcessDefinitionInfoCache().clear();
		deploymentCache.getProcessDefinitionCache().clear();
		DeploymentEntity deployment = deploymentBuilder.getDeployment();
	    deployment.setDeploymentTime(commandContext.getProcessEngineConfiguration().getClock().getCurrentTime());
		return deployment;
	}

	
}
