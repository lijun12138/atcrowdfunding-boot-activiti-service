package com.atguigu.atcrowdfunding.act;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableFeignClients
@EnableTransactionManagement
@MapperScan("com.atguigu.**.dao")
@EnableDiscoveryClient
@SpringBootApplication
public class AtcrowdfundingBootActivitiServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AtcrowdfundingBootActivitiServiceApplication.class, args);
	}
}
