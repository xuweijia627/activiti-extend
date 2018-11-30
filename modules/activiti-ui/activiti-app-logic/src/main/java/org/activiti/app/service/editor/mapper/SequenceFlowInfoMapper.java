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
package org.activiti.app.service.editor.mapper;

import java.util.List;
import java.util.Map;

import org.activiti.bpmn.model.ExtensionElement;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.editor.constants.StencilConstants;
import org.activiti.editor.language.json.converter.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class SequenceFlowInfoMapper extends AbstractInfoMapper {

	protected void mapProperties(Object element) {
		SequenceFlow sequenceFlow = (SequenceFlow) element;
		
		if (StringUtils.isNotEmpty(sequenceFlow.getConditionExpression())) {
		    //createPropertyNode("Condition expression", sequenceFlow.getConditionExpression());
			// 2018-11-28 add by xuWeiJia 查看详情的时候把条件表达式 和 名称展示出来
		    Map<String, List<ExtensionElement>> extensionElementMap= sequenceFlow.getExtensionElements();
		    List<ExtensionElement> expressionDisplayElements=(List<ExtensionElement>) extensionElementMap.get(StencilConstants.EXPRESSION_DISPLAY);
		    List<ExtensionElement> nameElements=(List<ExtensionElement>) extensionElementMap.get(StencilConstants.PROPERTY_NAME);
		    if(CollectionUtils.isNotEmpty(expressionDisplayElements)) {
		    	for(ExtensionElement ele : expressionDisplayElements) {
			    	createPropertyNode("Condition expression", ele.getElementText());
			    }
		    }
		    if(CollectionUtils.isNotEmpty(nameElements)) {
		    	for(ExtensionElement ele : nameElements) {
			    	createPropertyNode(StencilConstants.PROPERTY_NAME, ele.getElementText());
			    }
		    }
		    // add end
		}
		
		createListenerPropertyNodes("Execution listeners", sequenceFlow.getExecutionListeners());
	}
}
