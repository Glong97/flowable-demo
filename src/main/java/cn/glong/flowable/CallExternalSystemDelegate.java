package cn.glong.flowable;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * 当执行到达服务任务时，会初始化并调用BPMN 2.0 XML中所引用的类。
 * @description: 服务任务的实现
 * @author：Glong
 * @date: 2024/10/28
 */
public class CallExternalSystemDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        System.out.println("Calling the external system for employee "
                + execution.getVariable("employee"));
    }
}
