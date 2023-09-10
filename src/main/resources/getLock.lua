local key = KEYS[1] -- 锁的key
local threadId = ARGV[1] -- 线程唯一标识
local releaseTime = ARGV[2] -- 锁的释放时间

-- 判断锁是否存在
if (redis.call('exists',key) == 0) then
    -- 不存在，获取锁
    redis.call('hset',key,threadId,1)
    -- 设置过期时间
    redis.call('expire',key,releaseTime)
    return 1
end

-- 如果锁已经存在，判断是否是自己获取的锁
if (redis.call('hexists',key,threadId)) then
    -- 获取次数加1
    redis.call('hincrby',key,threadId,1)
    -- 重置过期时间
    redis.call('expire',key,releaseTime)
    return 1
end
-- 锁已经存在且不是自己获取的
return 0