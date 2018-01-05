package com.atguigu.atcrowdfunding.act.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.atguigu.atcrowdfunding.act.service.MemberService;
import com.atguigu.atcrowdfunding.common.bean.Member;
import com.atguigu.atcrowdfunding.common.bean.Ticket;

@Controller
@RestController
public class ActivitiController {

	@Autowired
	private RepositoryService repositoryService;

	
	@Autowired
	private RuntimeService runtimeService;
	
	@Autowired
	private TaskService taskService;
	
	@Autowired
	private MemberService memberService;
	
	@RequestMapping("act/queryData")
	List<Map<String, Object>> queryData(@RequestBody Map<String, Object> paramMap){
		List<Map<String,Object>> result = new ArrayList<>();
		//分页查出backckeck组的所有任务
		Integer startindex = (Integer) paramMap.get("startindex");
		Integer pagesize = (Integer) paramMap.get("pagesize");
		
		TaskQuery taskQuery = taskService.createTaskQuery();
		List<Task> listTask = taskQuery.taskCandidateGroup("backcheck").listPage(startindex, pagesize);
		long count = taskQuery.taskCandidateGroup("backcheck").count();
		
		
		for (Task task : listTask) {
			
			Map<String ,Object> taskMap = new HashMap<>();
			taskMap.put("count", count);
			taskMap.put("taskid", task.getId());
			taskMap.put("taskname", task.getName());
			
			String piid = task.getProcessInstanceId();
			Map<String,Object> param = new HashMap<>();
			param.put("piid", piid);
			Ticket ticket = memberService.queryTicket(param);
			
			Member member = memberService.queryMember(ticket.getMemberid());
			taskMap.put("memberid", member.getId());
			taskMap.put("loginacct", member.getLoginacct());
			
			String pdid = task.getProcessDefinitionId();
			ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
				.processDefinitionId(pdid)
				.latestVersion()
				.singleResult();
			
			taskMap.put("pdname", processDefinition.getName());
			taskMap.put("pdversion", processDefinition.getVersion());
			
			result.add(taskMap);
		}
		
		return result;
	}
	
	@RequestMapping("act/complete")
	void complete(@RequestBody Map<String, Object> variable) {
		if(variable.get("taskid")!=null) {
			taskService.complete((String)variable.get("taskid"),variable);
		}else {
			TaskQuery taskQuery = taskService.createTaskQuery();
			Task task = taskQuery.processInstanceId((String) variable.get("piid")).singleResult();
			taskService.complete(task.getId(),variable);
		}
	}
	
	/*
	 * 使用list集合发生的异常，每个ProcessDefinition对象在数据库中自关联，会发生死循环 Could not write JSON:
	 * Direct self-reference leading to cycle
	 * 
	 * 解决方案，将每个ProcessDefinition对象中我们需要的数据拿出来封装成Map集合
	 */
	@RequestMapping("act/startProcess")
	String startProcess(@RequestBody String loginacct) {
		ProcessDefinition processDefinition = repositoryService
				.createProcessDefinitionQuery()
				.processDefinitionKey("authflow")
				.latestVersion()
				.singleResult();
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("loginacct", loginacct);
		ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId(),paramMap);
		return processInstance.getId();
	}
	
	@RequestMapping("/act/delete/{id}")
	public void delete(@PathVariable("id") String id ) {
		repositoryService.deleteDeployment(id);
	}
	@RequestMapping("/act/loadImg/{id}")
	public byte[] loadImg(@PathVariable("id") String id) {
		
		//通过repositoryService拿到指定id的流程定义对象
		ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
		ProcessDefinition definition = processDefinitionQuery.processDefinitionId(id).singleResult();
		
		//通过调用getResourceAsStream方法获取到一个输入流
		//deploymentId : 部署对象的id
		//resourceName : 流程定义的资源名
		InputStream inputStream = repositoryService.getResourceAsStream(definition.getDeploymentId(), definition.getDiagramResourceName());
		//创建一个存放在内存中的输出流
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		//创建一个临时缓冲数组
		byte[] buff = new byte[1024*8];
		//一个标志
		int rc = 0;
		try {
			//调用read方法，一次读取buff长度的字节，当读取长度小于等于0时，文件读取完毕
			while((rc = inputStream.read(buff))>0) {
				//在读取的同时，传入多少长度就读取多少长度，存放在内存数组流中
				outStream.write(buff, 0, buff.length);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		//将输出流转换成一个字节数组，作为返回结果返回
		byte[] byteArray = outStream.toByteArray();
		return byteArray;
	}

	@RequestMapping("/act/deploy")
	public String deploy(@RequestParam("pdfile") MultipartFile file) {
		try {
			repositoryService.createDeployment()
			.addInputStream(file.getOriginalFilename(), file.getInputStream())
			.deploy();
			return "部署成功";
		} catch (IOException e) {
			e.printStackTrace();
			return "部署失败";
		}
	}
	
	@RequestMapping("/act/queryList")
	public List<Map<String, Object>> queryList(@RequestBody Map<String, Object> paramMap) {
		Integer startindex = (Integer) paramMap.get("startindex");
		Integer pagesize = (Integer) paramMap.get("pagesize");
		String queryText = (String) paramMap.get("queryText");

		ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
		List<ProcessDefinition> listPage = null;
		if (queryText != "") {
			// 如果查询条件不为空串时加模糊查询的分页查询
			listPage = query.processDefinitionNameLike(queryText).listPage(startindex, pagesize);
		} else {
			listPage = repositoryService.createProcessDefinitionQuery().listPage(startindex, pagesize);
		}
		// 如果查询条件为空串时不加模糊查询的分页查询
		List<Map<String, Object>> pdList = new ArrayList<>();
		for (ProcessDefinition processDefinition : listPage) {
			Map<String, Object> pdMap = new HashMap<>();
			pdMap.put("id", processDefinition.getId());
			pdMap.put("name", processDefinition.getName());
			pdMap.put("version", processDefinition.getVersion());
			pdMap.put("key", processDefinition.getKey());
			pdMap.put("deployid", processDefinition.getDeploymentId());
			pdList.add(pdMap);
		}
		return pdList;
	}

	/*
	 * @RequestMapping("/act/queryList/{startindex}/{pagesize}") public
	 * List<Map<String,Object>> queryList(@PathVariable("startindex") Integer
	 * startindex ,@PathVariable("pagesize") Integer pagesize){
	 * List<ProcessDefinition> listPage =
	 * repositoryService.createProcessDefinitionQuery().listPage(startindex,
	 * pagesize); List<Map<String,Object>> pdList = new ArrayList<>(); for
	 * (ProcessDefinition processDefinition : listPage) { Map<String,Object> pdMap =
	 * new HashMap<>(); pdMap.put("id", processDefinition.getId());
	 * pdMap.put("name", processDefinition.getName()); pdMap.put("version",
	 * processDefinition.getVersion()); pdMap.put("key",
	 * processDefinition.getKey()); pdMap.put("deployid",
	 * processDefinition.getDeploymentId()); pdList.add(pdMap); } return pdList; }
	 */
	@RequestMapping("/act/count")
	public int count(@RequestBody Map<String, Object> paramMap) {
		ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
		String queryText = (String) paramMap.get("queryText");
		long count = 0;
		if (queryText != "") {
			// 如果查询条件不为空串时加模糊查询的分页查询
			count = processDefinitionQuery.processDefinitionNameLike(queryText).count();
		} else {
			count = processDefinitionQuery.count();
		}
		return (int) count;
	}
}
