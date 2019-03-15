package org.activiti.engine.impl.el;

import org.apache.commons.lang3.StringUtils;

/**
 * @author xuWeiJia
 * @date 2018/12/18
 */
public class ExpressionFunctionUtils {
	public static final String GT = ">";
    public static final String LT = "<";
    public static final String EQ = "==";
    public static final String GE = ">=";
    public static final String LE = "<=";
    public static final String NE = "!=";
	/**
	 * 在指定比较符时 time与 当前时间相比较
	 * @param time
	 * @param operator 比较符
	 * @return
	 */
    public static boolean now(long time,CharSequence operator){
    	boolean flag=false;
    	if(StringUtils.isNotBlank(operator) && time != 0) {
    		long now=System.currentTimeMillis();
    		if (operator.equals(GT)) {
                flag = time > now;
            } else if (operator.equals(LT)) {
                flag = time < now;
            } else if (operator.equals(EQ)) {
                flag = time == now;
            } else if (operator.equals(GE)) {
                flag = time >= now;
            } else if (operator.equals(LE)) {
                flag = time <= now;
            } else if (operator.equals(NE)) {
                flag = time != now;
            }
    	}
        return flag;
    }
}
