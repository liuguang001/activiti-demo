import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.activiti.engine.FormService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Administrator
 *启动类
 */
public class ACTmain {
	public static Logger logger = LoggerFactory.getLogger(ACTmain.class);
	public static <V> void main(String[] args) throws ParseException {
		//创建流程引擎
		ProcessEngineConfiguration engineConfiguration = ProcessEngineConfiguration.createProcessEngineConfigurationFromResource("activiti.cfg.xml");
		ProcessEngine engine = engineConfiguration.buildProcessEngine();
		//部署流程定义
		RepositoryService repositoryService = engine.getRepositoryService();
		DeploymentBuilder builder = repositoryService.createDeployment();
		builder.addClasspathResource("./diagrams/second_approve.bpmn");
		Deployment deploy = builder.deploy();
		String id = deploy.getId();
		//获取流程定义对象
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(id).singleResult();
		String processName = processDefinition.getName();
		String processId= processDefinition.getId();
		//启动流程
		RuntimeService runtimeService = engine.getRuntimeService();
		ProcessInstance processInstance = runtimeService.startProcessInstanceById(processId);
		logger.info("启动了....{}",processInstance.getProcessDefinitionKey());
		FormService formService = engine.getFormService();
		while (processInstance!=null&&!processInstance.isEnded()) {
			TaskService taskService = engine.getTaskService();
			List<Task> list = taskService.createTaskQuery().list();
			Scanner scanner=new Scanner(System.in);
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
			Map<String, Object> variables = new HashMap<String,Object>();
			for (Task task : list) {
				TaskFormData taskFormData = formService.getTaskFormData(task.getId());
				List<FormProperty> formProperties = taskFormData.getFormProperties();
				for (FormProperty formProperty : formProperties) {
					String nextLine=null;
					if (StringFormType.class.isInstance(formProperty.getType())) {
						logger.info("请输出:"+formProperty.getName());
						nextLine = scanner.nextLine();
						variables.put(formProperty.getId(), nextLine);
					}else if(DateFormType.class.isInstance(formProperty.getType())){
						logger.info("请输出,格式为yyyy-MM-dd"+formProperty.getName());
						nextLine= scanner.nextLine();
						Date parse = simpleDateFormat.parse(nextLine);
						variables.put(formProperty.getName(), parse);
					}else {
						logger.error("属性数据类型错误");
					}
					logger.info("你输入的结果为:"+nextLine);
				}
				taskService.complete(task.getId(), variables);
			}
			processInstance=runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
		}
	}

}
