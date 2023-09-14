package com.rddp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
@Slf4j
public class CacheUtils {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 将Java对象设置为有过期时间的redis类型
     * @param key
     * @param value
     * @param time
     * @param unit
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }

    /**
     * 通过逻辑过期解决缓存击穿
     * @param key
     * @param value
     * @param time
     * @param unit
     */
    public void setWithLogicalThrough(String key,Object value,Long time,TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(value));
    }

    /**
     * 存储空数据解决缓存穿透
     * @param keyPrefix
     * @param id
     * @param type
     * @param dbFallback 查询数据库的回调函数
     * @param time
     * @param unit
     * @return
     * @param <R>
     * @param <ID>
     */
    public <R,ID> R queryWithCacheThrough(String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback,
                                          Long time,TimeUnit unit) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            R r = JSONUtil.toBean(json, type);
            return r;
        }

        if ("".equals(json)) {
            return null;
        }

        R r = dbFallback.apply(id);
        if (r == null) {
            stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
            return null;
        }
        this.set(key,JSONUtil.toJsonStr(r),time,unit);
        return r;
    }

    public <R,ID> R queryWithLogicalThrough(String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback,
                                            Long time,TimeUnit unit) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(json)) {
            return null;
        }
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        // 如果未逻辑过期，直接返回数据
        if (LocalDateTime.now().isAfter(redisData.getExpireTime())) {
            return r;
        }

        // 如果逻辑过期，进行缓存重建
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        try {
            boolean isLock = tryLock(lockKey);
            if (!isLock) {
                return queryWithLogicalThrough(keyPrefix,id,type,dbFallback,time,unit);
            }
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                R r1 = dbFallback.apply(id);
                this.setWithLogicalThrough(key,r1,time,unit);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            unlock(lockKey);
        }
        return r;
    }

    private boolean tryLock(String key) {
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(key, "1",
                RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(lock);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
