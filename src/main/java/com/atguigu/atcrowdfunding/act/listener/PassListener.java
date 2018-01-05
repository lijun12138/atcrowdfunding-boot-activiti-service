package com.atguigu.atcrowdfunding.act.listener;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;

public class PassListener implements ExecutionListener {
	private static final long serialVersionUID = 1L;

	@Override
	public void notify(DelegateExecution execution) throws Exception {
		System.out.println("审核通过...");

	}

}
