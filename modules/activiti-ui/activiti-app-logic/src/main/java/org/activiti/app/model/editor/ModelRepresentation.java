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
package org.activiti.app.model.editor;

import java.util.Date;
import java.util.Set;

import org.activiti.app.domain.editor.AbstractModel;
import org.activiti.app.domain.editor.Model;
import org.activiti.app.domain.editor.ModelHistory;
import org.activiti.app.model.common.AbstractRepresentation;

/**
 * Representation of process-models, both current and historic models.
 * 
 * @author Tijs Rademakers
 */
public class ModelRepresentation extends AbstractRepresentation {

  protected String id;
  protected String name;
  protected String key;
  protected String description;
  protected String createdBy;
  protected String lastUpdatedBy;
  protected Date lastUpdated;
  protected boolean latestVersion;
  protected int version;
  protected String comment;
  protected Integer modelType;
  protected Long organizationId;
  protected String processType;
  protected Boolean template;
  protected String templateId;

  public ModelRepresentation(AbstractModel model) {
    initialize(model);
  }

  public ModelRepresentation() {

  }

  public void initialize(AbstractModel model) {
    this.id = model.getId();
    this.name = model.getName();
    this.key = model.getKey();
    this.description = model.getDescription();
    this.createdBy = model.getCreatedBy();
    this.lastUpdated = model.getLastUpdated();
    this.version = model.getVersion();
    this.lastUpdatedBy = model.getLastUpdatedBy();
    this.comment = model.getComment();
    this.modelType = model.getModelType();

    // When based on a ProcessModel and not history, this is always the latest version
    if (model instanceof Model) {
      this.setLatestVersion(true);
      this.setOrganizationId(((Model) model).getOrganizationId());
      this.setProcessType(((Model) model).getProcessType());
      this.setTemplate(((Model) model).getTemplate());
      this.setTemplateId(((Model) model).getTemplateId());
    } else if (model instanceof ModelHistory) {
      this.setLatestVersion(false);
    }
  }

  
 

public String getTemplateId() {
	return templateId;
}

public void setTemplateId(String templateId) {
	this.templateId = templateId;
}

public Boolean getTemplate() {
	return template;
}

public void setTemplate(Boolean template) {
	this.template = template;
}

public String getProcessType() {
	return processType;
}

public void setProcessType(String processType) {
	this.processType = processType;
}

public Long getOrganizationId() {
	return organizationId;
}

public void setOrganizationId(Long organizationId) {
	this.organizationId = organizationId;
}

public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Date getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public void setLatestVersion(boolean latestVersion) {
    this.latestVersion = latestVersion;
  }

  public boolean isLatestVersion() {
    return latestVersion;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public String getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  public void setLastUpdatedBy(String lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getComment() {
    return comment;
  }

  public Integer getModelType() {
    return modelType;
  }

  public void setModelType(Integer modelType) {
    this.modelType = modelType;
  }

  public Model toModel() {
    Model model = new Model();
    model.setName(name);
    model.setDescription(description);
    return model;
  }

  /**
   * Update all editable properties of the given {@link Model} based on the values in this instance.
   */
  public void updateModel(Model model) {
    model.setDescription(this.description);
    model.setName(this.name);
  }
}
