package com.atguigu.atcrowdfunding.act.service;

import java.util.Map;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.atguigu.atcrowdfunding.common.bean.Member;
import com.atguigu.atcrowdfunding.common.bean.Ticket;

@FeignClient("atcrowdfunding-member-service")
public interface MemberService {

	@RequestMapping("member/queryTicket")
	Ticket queryTicket(@RequestBody Map<String, Object> param);

	@RequestMapping("member/queryMember")
	Member queryMember(@RequestBody Integer memberid);


}
