package com.rddp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{

    private StringRedisTemplate stringRedisTemplate;
    private String name;
    /**
     * redis key的前缀
     */
    private static final String KEY_PREFIX = "lock";
    /**
     * 存到redis中的value值的前缀，uuid+threadId
     */
    private static final String ID_PREFIX = UUID.randomUUID().toString(true)+"-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }


    /**
     * 获取分布式锁
     * @param timeoutSec 过期时间
     * @return true代表获取成功，false代表获取失败
     */
    @Override
    public boolean tryLock(long timeoutSec) {
        String id = ID_PREFIX+Thread.currentThread().getId();
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, id, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 调用lua脚本释放锁
     */
    @Override
    public void unlock() {
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX+name),
                ID_PREFIX+Thread.currentThread().getId()
                );
    }

//    @Override
//    public void unlock() {
//        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
//        String threadId = ID_PREFIX+Thread.currentThread().getId();
//        if (!threadId.equals(id)) {
//            stringRedisTemplate.delete(KEY_PREFIX+name);
//        }
//    }
}
