package org.activiti.engine.impl.el;

import org.apache.commons.lang3.StringUtils;

/**
 * @author xuWeiJia
 * @date 2018/12/18
 */
public class ExpressionFunctionUtils {

	/**
	 * 在指定比较符时 time与 当前时间相比较
	 * @param time
	 * @param operator 比较符
	 * @return
	 */
    public static boolean now(long time,CharSequence operator){
    	boolean flag=false;
    	if(StringUtils.isNotBlank(operator)) {
    		long now=System.currentTimeMillis();
    		if(operator.equals(">")) {
    			flag=time>now;
    		}else if(operator.equals("<")) {
    			flag=time<now;
    		}else if(operator.equals("==")) {
    			flag=time==now;
    		}
    	}
        return flag;
    }
}
