package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final long BEGIN_TIME = 1577836800L;
    private static final int COUNT_BITS = 32;

    /**
     * 生成全局唯一id值
     * @param keyPrefix
     * @return
     */
    public long nextId(String keyPrefix) {
        LocalDateTime now = LocalDateTime.now();
        long timestamp = now.toEpochSecond(ZoneOffset.UTC) - BEGIN_TIME;
        String format = now.format(DateTimeFormatter.ofPattern("yy:MM:dd"));
        Long id = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + format);
        return timestamp << COUNT_BITS | id;
    }

}
