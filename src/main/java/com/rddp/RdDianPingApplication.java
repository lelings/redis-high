package com.rddp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.rddp.mapper")
@SpringBootApplication
public class RdDianPingApplication {

    public static void main(String[] args) {
        SpringApplication.run(RdDianPingApplication.class, args);
    }

}
