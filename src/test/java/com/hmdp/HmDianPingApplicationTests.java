package com.hmdp;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

@SpringBootTest
public class HmDianPingApplicationTests {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Test
    public void testCreateRedisStream() {

    }


}
