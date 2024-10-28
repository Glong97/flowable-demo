package cn.glong.flowable;

import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * @description: 请假流程-demo
 * @author：Glong
 * @date: 2024/10/28
 */
public class HolidayRequest {

    public static void main(String[] args) {
        // 一、配置流程引擎
        // 1. 初始化ProcessEngine流程引擎实例
        // 1.1 - 初始化引擎配置对象
        ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:flowable;DB_CLOSE_DELAY=-1")
                .setJdbcUsername("sa")
                .setJdbcPassword("")
                .setJdbcDriver("org.h2.Driver")
                // 设置了true，确保在JDBC参数连接的数据库中，数据库表结构不存在时，会创建相应的表结构。
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        // 1.2 - 通过引擎配置对象创建初始化流程引擎对象
        ProcessEngine processEngine = cfg.buildProcessEngine();

        // 2. 有了流程BPMN 2.0 XML文件，需要将它部署(deploy)到引擎中。
        // 2.1 - 从ProcessEngine对象获取RepositoryService对象
        RepositoryService repositoryService = processEngine.getRepositoryService();
        // 2.2 - 通过XML文件的路径创建一个新的部署(Deployment)，并调用deploy()方法实际执行。
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("holiday-request.bpmn20.xml")
                .deploy();

        // 3. 通过API查询验证流程定义已经部署在引擎中
        // 3.1 - 通过RepositoryService创建的ProcessDefinitionQuery对象实现。
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
        // 3.2 - 查询输出流程名称
        System.out.println("Found process definition : " + processDefinition.getName());

        // 二、启动并执行流程实例
        // 1. 提供一些初始化流程变量
        Scanner scanner= new Scanner(System.in);

        System.out.println("Who are you?");
        String employee = scanner.nextLine();

        System.out.println("How many holidays do you want to request?");
        Integer nrOfHolidays = Integer.valueOf(scanner.nextLine());

        System.out.println("Why do you need them?");
        String description = scanner.nextLine();
        // 2. 使用RuntimeService启动一个流程实例
        // 2.1 - 获取RuntimeService对象
        RuntimeService runtimeService = processEngine.getRuntimeService();
        // 2.2 - 收集的数据作为一个java.util.Map实例传递，其中的键就是之后用于获取变量的标识符。
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("employee", employee);
        variables.put("nrOfHolidays", nrOfHolidays);
        variables.put("description", description);
        // 2.3 - 这个流程实例使用key启动。这个key就是BPMN 2.0 XML文件中设置的id属性，在这个例子里是holidayRequest。
        // 在流程实例启动后，会创建一个执行(execution)，并将其放在启动事件上。
        // 从这里开始，这个执行沿着顺序流移动到经理审批的用户任务，并执行用户任务行为。
        // 这个行为将在数据库中创建一个任务，该任务可以之后使用查询找到。
        // 用户任务是一个等待状态(wait state)，引擎会停止执行，返回API调用处。
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("holidayRequest", variables);
        // 3. 获得实际的任务列表，需要通过TaskService创建一个TaskQuery。配置这个查询只返回’managers’组的任务:
        TaskService taskService = processEngine.getTaskService();
        List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup("managers").list();
        System.out.println("You have " + tasks.size() + " tasks:");
        for (int i=0; i<tasks.size(); i++) {
            System.out.println((i+1) + ") " + tasks.get(i).getName());
        }
        // 4. 使用任务Id获取特定流程实例的变量，并在屏幕上显示实际的申请:
        System.out.println("Which task would you like to complete?");
        int taskIndex = Integer.valueOf(scanner.nextLine());
        Task task = tasks.get(taskIndex - 1);
        Map<String, Object> processVariables = taskService.getVariables(task.getId());
        System.out.println(processVariables.get("employee") + " wants " +
                processVariables.get("nrOfHolidays") + " of holidays. Do you approve this?");
        // 5. 完成任务。在现实中，这通常意味着由用户提交一个表单。表单中的数据作为流程变量传递。
        // 在这里，我们在完成任务时传递带有’approved’变量（这个名字很重要，因为之后会在顺序流的条件中使用！）的map来模拟：
        // 现在任务完成，并会在离开排他网关的两条路径中，基于’approved’流程变量选择一条。
        boolean approved = scanner.nextLine().toLowerCase().equals("y");
        variables = new HashMap<String, Object>();
        variables.put("approved", approved);
        taskService.complete(task.getId(), variables);

        // 三、使用历史数据
        // 1. 从ProcessEngine获取HistoryService。
        HistoryService historyService = processEngine.getHistoryService();
        // 2. 创建历史活动(historical activities)的查询
        //    - 只选择一个特定流程实例的活动
        //    - 只选择已完成的活动
        //    - 结果按照结束时间排序，代表其执行顺序。
        List<HistoricActivityInstance> activities =
                historyService.createHistoricActivityInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .finished()
                        .orderByHistoricActivityInstanceEndTime().asc()
                        .list();
        // 3. 输出历史数据信息
        for (HistoricActivityInstance activity : activities) {
            System.out.println(activity.getActivityId() + " took "
                    + activity.getDurationInMillis() + " milliseconds");
        }
    }
}
