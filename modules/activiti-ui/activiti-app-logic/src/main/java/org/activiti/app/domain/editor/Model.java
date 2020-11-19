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
package org.activiti.app.domain.editor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="ACT_DE_MODEL")
public class Model extends AbstractModel {

	@Column(name="thumbnail")
	private byte[] thumbnail;
	// add by xuWeiJia
	@Column(name = "tenant_id")
    private String tenantId;
	@Column(name = "organization_id")
    private Long organizationId;
	@Column(name = "created_by_user_id")
	private String createdByUserId;
	/** 流程类型包括两种 approval :审批    processAudit 流程审核 */
	@Column(name = "process_type")
	protected String processType;
	/** 1: 模板流程  0：普通流程*/
	@Column(name = "template")
	protected Boolean template;
	/** 新建流程的时候可以选择一个模板，templateId表示选择的那个模板id*/
	@Column(name = "template_id")
	protected String templateId;
	/** 对每个流程都可以进行复制，copyFromModelId 表示从哪个流程复制过来的*/
	@Column(name = "copy_from_model_id")
	protected String copyFromModelId;
	/** 对每个流程都可以进行复制，needCopyForm表示是否需要复制流程节点上关联的表单*/
	@Column(name = "need_copy_form")
	protected Boolean needCopyForm;
	// add end
	
	public Model() {
		super();
	}
	
	public byte[] getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(byte[] thumbnail) {
		this.thumbnail = thumbnail;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public Long getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(Long organizationId) {
		this.organizationId = organizationId;
	}

	public String getCreatedByUserId() {
		return createdByUserId;
	}

	public void setCreatedByUserId(String createdByUserId) {
		this.createdByUserId = createdByUserId;
	}

	public String getProcessType() {
		return processType;
	}

	public void setProcessType(String processType) {
		this.processType = processType;
	}

	public Boolean getTemplate() {
		return template;
	}

	public void setTemplate(Boolean template) {
		this.template = template;
	}

	public String getTemplateId() {
		return templateId;
	}

	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

	public String getCopyFromModelId() {
		return copyFromModelId;
	}

	public void setCopyFromModelId(String copyFromModelId) {
		this.copyFromModelId = copyFromModelId;
	}

	public Boolean getNeedCopyForm() {
		return needCopyForm;
	}

	public void setNeedCopyForm(Boolean needCopyForm) {
		this.needCopyForm = needCopyForm;
	}
}