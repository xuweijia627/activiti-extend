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
package org.activiti.app.rest.editor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import org.activiti.app.domain.editor.Model;
import org.activiti.app.model.editor.ModelKeyRepresentation;
import org.activiti.app.model.editor.ModelRepresentation;
import org.activiti.app.repository.editor.ModelRepository;
import org.activiti.app.security.SecurityUtils;
import org.activiti.app.service.api.ModelService;
import org.activiti.app.service.exception.BadRequestException;
import org.activiti.app.service.exception.ConflictingRequestException;
import org.activiti.app.service.exception.InternalServerErrorException;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.editor.constants.EditorJsonConstants;
import org.activiti.editor.constants.StencilConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.identity.User;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

@RestController
public class ModelResource extends AbstractModelResource implements StencilConstants, EditorJsonConstants {

  private static final Logger log = LoggerFactory.getLogger(ModelResource.class);

  private static final String RESOLVE_ACTION_OVERWRITE = "overwrite";
  private static final String RESOLVE_ACTION_SAVE_AS = "saveAs";
  private static final String RESOLVE_ACTION_NEW_VERSION = "newVersion";

  @Autowired
  protected ModelService modelService;
  
  @Autowired
  protected ModelRepository modelRepository;
  
  @Autowired
  protected ObjectMapper objectMapper;
  @Autowired
  private RepositoryService repositoryService;

  protected BpmnJsonConverter bpmnJsonConverter = new BpmnJsonConverter();

  protected BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();

  /**
   * GET /rest/models/{modelId} -> Get process model
   */
  @RequestMapping(value = "/rest/models/{modelId}", method = RequestMethod.GET, produces = "application/json")
  public ModelRepresentation getModel(@PathVariable String modelId) {
    return super.getModel(modelId);
  }

  /**
   * GET /rest/models/{modelId}/thumbnail -> Get process model thumbnail
   */
  @RequestMapping(value = "/rest/models/{modelId}/thumbnail", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
  public byte[] getModelThumbnail(@PathVariable String modelId) {
    return super.getModelThumbnail(modelId);
  }

  /**
   * PUT /rest/models/{modelId} -> update process model properties
   */
  @RequestMapping(value = "/rest/models/{modelId}", method = RequestMethod.PUT)
  public ModelRepresentation updateModel(@PathVariable String modelId, @RequestBody ModelRepresentation updatedModel) {
    // Get model, write-permission required if not a favorite-update
    Model model = modelService.getModel(modelId);
    
    ModelKeyRepresentation modelKeyInfo = modelService.validateModelKey(model, model.getModelType(), updatedModel.getKey());
    if (modelKeyInfo.isKeyAlreadyExists()) {
      throw new BadRequestException("Model with provided key already exists " + updatedModel.getKey());
    }

    try {
      updatedModel.updateModel(model);
      modelRepository.save(model);
      
      ModelRepresentation result = new ModelRepresentation(model);
      return result;

    } catch (Exception e) {
      throw new BadRequestException("Model cannot be updated: " + modelId);
    }
  }

  /**
   * DELETE /rest/models/{modelId} -> delete process model or, as a non-owner, remove the share info link for that user specifically
   */
  @ResponseStatus(value = HttpStatus.OK)
  @RequestMapping(value = "/rest/models/{modelId}", method = RequestMethod.DELETE)
  public void deleteModel(@PathVariable String modelId, @RequestParam(required = false) Boolean cascade, @RequestParam(required = false) Boolean deleteRuntimeApp) {

    // Get model to check if it exists, read-permission required for delete (in case user is not owner, only share info
    // will be deleted
    Model model = modelService.getModel(modelId);

    try {
      String currentUserId = SecurityUtils.getCurrentUserId();
      boolean currentUserIsOwner = currentUserId.equals(model.getCreatedBy());
      if (currentUserIsOwner) {
        modelService.deleteModel(model.getId(), Boolean.TRUE.equals(cascade), Boolean.TRUE.equals(deleteRuntimeApp));
      }

    } catch (Exception e) {
      log.error("Error while deleting: ", e);
      throw new BadRequestException("Model cannot be deleted: " + modelId);
    }
  }

  public static JSONObject conditionExpressionConvert(JSONObject modelObject) {
    JSONArray childShapes = modelObject.getJSONArray(EditorJsonConstants.EDITOR_CHILD_SHAPES);
    for (Object childShape : childShapes) {
      JSONObject child = (JSONObject) childShape;
      JSONObject properties = child.getJSONObject(EditorJsonConstants.EDITOR_SHAPE_PROPERTIES);
      JSONObject stencil = child.getJSONObject(EditorJsonConstants.EDITOR_STENCIL);
      if (stencil != null && stencil.getString(EditorJsonConstants.EDITOR_STENCIL_ID).equals(StencilConstants.STENCIL_SUB_PROCESS)) {
        conditionExpressionConvert(child);
      }
      if (properties != null) {
        // 条件表达式
        JSONObject condition = properties.getJSONObject(StencilConstants.PROPERTY_SEQUENCEFLOW_CONDITION);
        if (condition != null && !condition.isEmpty()) {
          JSONObject expression = condition.getJSONObject(StencilConstants.PROPERTY_FIELD_EXPRESSION);
          if (expression != null && !expression.isEmpty()) {
            String staticValue = expression.getString("staticValue");
            if (staticValue != null) {
              condition.put(StencilConstants.PROPERTY_FIELD_EXPRESSION, staticValue);
            }
          }
        }
      }
    }
    return modelObject;
  }

  /**
   * GET /rest/models/{modelId}/editor/json -> get the JSON model
   */
  @RequestMapping(value = "/rest/models/{modelId}/editor/json", method = RequestMethod.GET, produces = "application/json")
  public JSONObject getModelJSON(@PathVariable String modelId) {
    Model model = modelService.getModel(modelId);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("modelId", model.getId());
    jsonObject.put("name", model.getName());
    jsonObject.put("key", model.getKey());
    jsonObject.put("description", model.getDescription());
    jsonObject.put("lastUpdated", model.getLastUpdated());
    jsonObject.put("lastUpdatedBy", model.getLastUpdatedBy());
    jsonObject.put("organizationId",model.getOrganizationId());
    jsonObject.put("processType",model.getProcessType());
    jsonObject.put("copyFromModelId",model.getCopyFromModelId());
    jsonObject.put("needCopyForm",model.getNeedCopyForm());
    jsonObject.put("template",model.getTemplate());
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionTenantId("brs")
            .processDefinitionKey(model.getKey()).latestVersion().singleResult();
    Date deployTime=null;
    if(processDefinition!=null){
      Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(processDefinition.getDeploymentId())
              .singleResult();
      if(deployment!=null){
        deployTime=deployment.getDeploymentTime();
      }
    }
    jsonObject.put("deployTime",deployTime);
    if (StringUtils.isNotEmpty(model.getModelEditorJson())) {
      try {
        JSONObject modelObject =(JSONObject) JSON.parse(model.getModelEditorJson());
        conditionExpressionConvert(modelObject);
        modelObject.put("modelType", "model");
        jsonObject.put("model", modelObject);
      } catch (Exception e) {
        log.error("Error reading editor json " + modelId, e);
        throw new InternalServerErrorException("Error reading editor json " + modelId);
      }
    } else {
      JSONObject modelObject = new JSONObject();
      modelObject.put("id", "canvas");
      modelObject.put("resourceId", "canvas");
      modelObject.put("modelType", "model");
      jsonObject.put("model", modelObject);
    }
    return jsonObject;
  }

  /**
   * POST /rest/models/{modelId}/editor/json -> save the JSON model
   */
  @RequestMapping(value = "/rest/models/{modelId}/editor/json", method = RequestMethod.POST)
  public Object saveModel(@PathVariable String modelId, @RequestBody MultiValueMap<String, String> values) {

    // Validation: see if there was another update in the meantime
    long lastUpdated = -1L;
    String lastUpdatedString = values.getFirst("lastUpdated");
    if (lastUpdatedString == null) {
      throw new BadRequestException("Missing lastUpdated date");
    }
    try {
      Date readValue = objectMapper.getDeserializationConfig().getDateFormat().parse(lastUpdatedString);
      lastUpdated = readValue.getTime();
    } catch (ParseException e) {
      throw new BadRequestException("Invalid lastUpdated date: '" + lastUpdatedString + "'");
    }

    Model model = modelService.getModel(modelId);
    User currentUser = SecurityUtils.getCurrentUserObject();
    boolean currentUserIsOwner = model.getLastUpdatedBy().equals(currentUser.getId());
    String resolveAction = values.getFirst("conflictResolveAction");

    // If timestamps differ, there is a conflict or a conflict has been resolved by the user
    if (model.getLastUpdated().getTime() != lastUpdated) {

      if (RESOLVE_ACTION_SAVE_AS.equals(resolveAction)) {

        String saveAs = values.getFirst("saveAs");
        String json = values.getFirst("json_xml");
        return createNewModel(saveAs, model.getDescription(), model.getModelType(), json);

      } else if (RESOLVE_ACTION_OVERWRITE.equals(resolveAction)) {
        return updateModel(model, values, false);
      } else if (RESOLVE_ACTION_NEW_VERSION.equals(resolveAction)) {
        return updateModel(model, values, true);
      } else {

        // Exception case: the user is the owner and selected to create a new version
        String isNewVersionString = values.getFirst("newversion");
        if (currentUserIsOwner && "true".equals(isNewVersionString)) {
          return updateModel(model, values, true);
        } else {
          // Tried everything, this is really a conflict, return 409
          /*ConflictingRequestException exception = new ConflictingRequestException("Process model was updated in the meantime");
          exception.addCustomData("userFullName", model.getLastUpdatedBy());
          exception.addCustomData("newVersionAllowed", currentUserIsOwner);
          throw exception;*/
          JSONObject response = new JSONObject();
          response.put("code", 409);
          response.put("message", "流程版本冲突");
          response.put("messageI18nCode", "WORKFLOWAPP_VERSION_CONFLICT");
          JSONArray data = new JSONArray();
          JSONObject reason = new JSONObject();
          reason.put("name", values.getFirst("name"));
          reason.put("typeName", "process");
          reason.put("rejectReason", "流程版本冲突,请刷新之后再修改");
          reason.put("reasonCode", "WORKFLOWAPP_VERSION_CONFLICT_REASON");
          data.add(reason);
          response.put("data", data);
          return response;
        }
      }

    } else {

      // Actual, regular, update
      return updateModel(model, values, false);

    }
  }

  /**
   * POST /rest/models/{modelId}/editor/newversion -> create a new model version
   */
  @RequestMapping(value = "/rest/models/{modelId}/newversion", method = RequestMethod.POST)
  public ModelRepresentation importNewVersion(@PathVariable String modelId, @RequestParam("file") MultipartFile file) {
    return super.importNewVersion(modelId, file);
  }
  
  protected ModelRepresentation updateModel(Model model, MultiValueMap<String, String> values, boolean forceNewVersion) {

    String name = values.getFirst("name");
    String key = values.getFirst("key");
    String description = values.getFirst("description");
    String isNewVersionString = values.getFirst("newversion");
    String newVersionComment = null;
    
    ModelKeyRepresentation modelKeyInfo = modelService.validateModelKey(model, model.getModelType(), key);
    if (modelKeyInfo.isKeyAlreadyExists()) {
      throw new BadRequestException("Model with provided key already exists " + key);
    }

    boolean newVersion = false;
    if (forceNewVersion) {
      newVersion = true;
      newVersionComment = values.getFirst("comment");
    } else {
      if (isNewVersionString != null) {
        newVersion = "true".equals(isNewVersionString);
        newVersionComment = values.getFirst("comment");
      }
    }

    String json = values.getFirst("json_xml");
    try {
      JsonNode modelNode = objectMapper.readTree(json);
      modelNode=addCustomProperties(modelNode);
      json=modelNode.toString();
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      model = modelService.saveModel(model.getId(), name, key, description, json, newVersion, 
          newVersionComment, SecurityUtils.getCurrentUserObject());
      return new ModelRepresentation(model);
      
    } catch (Exception e) {
      log.error("Error saving model " + model.getId(), e);
      throw new BadRequestException("Process model could not be saved " + model.getId());
    }
  }

  protected ModelRepresentation createNewModel(String name, String description, Integer modelType, String editorJson) {
    ModelRepresentation model = new ModelRepresentation();
    model.setName(name);
    model.setDescription(description);
    model.setModelType(modelType);
    Model newModel = modelService.createModel(model, editorJson, SecurityUtils.getCurrentUserObject());
    return new ModelRepresentation(newModel);
  }

  public JsonNode addCustomProperties(JsonNode modelNode) {
    JsonNode processProperties = modelNode.get(EDITOR_SHAPE_PROPERTIES);
    JsonNode slaNode = processProperties.get(SLA);
    if (notBlank(slaNode)) {
      parseTimeStr(slaNode);
    }
    JsonNode sptNode = processProperties.get(SPT);
    if (notBlank(sptNode)) {
      parseTimeStr(sptNode);
    }

    ArrayNode childShapes = (ArrayNode) modelNode.get(EditorJsonConstants.EDITOR_CHILD_SHAPES);
    // 流程中参与流转条件判断的key
    List<String> keys = Lists.newArrayList();
    ObjectNode startNodeProperties = null;
    for (JsonNode childNode : childShapes) {
      ObjectNode properties = (ObjectNode) childNode.get(EditorJsonConstants.EDITOR_SHAPE_PROPERTIES);
      ObjectNode stencil = (ObjectNode) childNode.get(EditorJsonConstants.EDITOR_STENCIL);
      String elementName = stencil.get(EditorJsonConstants.EDITOR_STENCIL_ID).textValue();
      if (elementName.equals(StencilConstants.STENCIL_EVENT_START_NONE)) {
        startNodeProperties = properties;
      } else if (elementName.equals(StencilConstants.STENCIL_SEQUENCE_FLOW)) {
        // 条件表达式
        JsonNode condition = properties.get(StencilConstants.PROPERTY_SEQUENCEFLOW_CONDITION);
        if (notBlank(condition)) {
          ObjectNode conditionObj = (ObjectNode) condition;
          if (conditionObj.get(StencilConstants.PROPERTY_FIELD_EXPRESSION) != null) {
            ObjectNode expressionNode = objectMapper.createObjectNode();
            String conditionValue = conditionObj.get(StencilConstants.PROPERTY_FIELD_EXPRESSION).textValue();

            if (StringUtils.isNotBlank(conditionValue)) {
              ArrayNode conditionArray = (ArrayNode) conditionObj.get("data");
              for (JsonNode jsonNode : conditionArray) {
                String value = jsonNode.get("value").asText();
                String conditionKey = jsonNode.get("key").asText();
                if (StringUtils.isNotBlank(value) && StringUtils.isNotBlank(conditionKey)) {
                  keys.add(conditionKey);
                  String dateFormatStr = dateFormatStr(value);
                  if (StringUtils.isNotBlank(dateFormatStr)) {
                    Long time = parseToLong(value, dateFormatStr);
                    if (time != null) {
                      String key = jsonNode.get("key").asText();
                      String symbol = jsonNode.get("symbol").asText();
                      String oldCondition = key + symbol + value;
                      String newCondition = key + symbol + time;
                      conditionValue = conditionValue.replace(oldCondition, newCondition);
                    }
                  }
                }
              }
            }
            expressionNode.put("type", "static");
            expressionNode.put("staticValue", conditionValue);
            conditionObj.replace(StencilConstants.PROPERTY_FIELD_EXPRESSION, expressionNode);
          }
        }
      } else if (elementName.equals(StencilConstants.STENCIL_EVENT_BOUNDARY_TIMER)) {
        properties.put(StencilConstants.PROPERTY_CANCEL_ACTIVITY, true);
      } else if (elementName.equals(StencilConstants.STENCIL_TASK_SERVICE)){
        properties.put(StencilConstants.PROPERTY_SERVICETASK_CLASS, "com.brs.activiti.service.delegate.RpaTaskDelegate");
      } else if (elementName.equals(StencilConstants.STENCIL_SUB_PROCESS)) {
        addCustomProperties(childNode);
      }
    }
    if (startNodeProperties != null) {
      startNodeProperties.put("conditionKeys", StringUtils.join(keys, ","));
    }
    return modelNode;
  }

  private boolean notBlank(JsonNode jsonNode){
    return jsonNode != null
            && StringUtils.isNotBlank(jsonNode.toString())
            && !DOUBLE_QUOTES.equals(jsonNode.toString())
            && !jsonNode.getNodeType().equals(JsonNodeType.NULL);
  }
  private Long parseToLong(String value, String dateFormatStr){
    Long time = null;
    if (dateFormatStr.equals(TIME_FORMAT)){
      String[] timeArr = value.split(":");
      time = Long.valueOf(timeArr[0]) * 60 * 60 + Long.valueOf(timeArr[1]) * 60 + Long.valueOf(timeArr[2]);
    } else {
      try {
        Date date = DateUtils.parseDate(value.trim(), dateFormatStr);
        time = date.getTime();
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
    return time;
  }
  private JsonNode parseTimeStr(JsonNode jsonNode){
    if (jsonNode instanceof TextNode) {
      try {
        jsonNode = this.objectMapper.readTree(jsonNode.asText());
        if (jsonNode instanceof TextNode) {
          jsonNode = objectMapper.readTree(jsonNode.asText());
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    if (!notBlank(jsonNode)) {
      return jsonNode;
    }
    ArrayNode slaArray = (ArrayNode) jsonNode;
    for (JsonNode sla : slaArray) {
      ArrayNode expressionArray = (ArrayNode) sla.get("expression");
      String conditionValue = sla.get("condition").asText();
      for (JsonNode expression : expressionArray) {
        String value = expression.get("value").asText();
        String dateFormatStr = dateFormatStr(value);
        if (StringUtils.isNotBlank(dateFormatStr)) {
          Long time = parseToLong(value, dateFormatStr);
          if (time != null) {
            String key = expression.get("key").asText();
            String symbol = expression.get("symbol").asText();
            String oldCondition = key + symbol + value;
            String newCondition = key + symbol + time;
            conditionValue = conditionValue.replace(oldCondition, newCondition);
            ((ObjectNode) sla).put("condition", conditionValue);
          }
        }
      }
    }
    return jsonNode;
  }

  private String dateFormatStr(String value){
    String formatStr = null;
    if (isDate(value)) {
      formatStr = DATE_FORMAT;
    } else if (isDateTime(value)) {
      formatStr = DATE_TIME_FORMAT;
    } else if (isTime(value)) {
      formatStr = TIME_FORMAT;
    }
    return formatStr;
  }
  public boolean isDate(String value) {
    return value.matches(DATE_REGEX);
  }
  public boolean isDateTime(String value) {
    return value.matches(DATETIME_REGEX);
  }

  public boolean isTime(String value) {
    return value.matches(TIME_REGEX);
  }
  public static final String DOUBLE_QUOTES = "\"\"";
  public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
  public static final String DATE_FORMAT = "yyyy-MM-dd";
  public static final String TIME_FORMAT = "HH:mm:ss";
  public static final String DATE_REGEX = "\\d{4}-\\d{2}-\\d{2}";
  /**
   * 日期时间类型字符串匹配
   */
  public static final String DATETIME_REGEX = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";
  public static final String TIME_REGEX = "\\d{2}:\\d{2}:\\d{2}";
}
