local key = KEYS[1]
local threadId = ARGV[1]
local releaseTime = ARGV[2]

-- 判断锁是否存在
if (redis.call('HEXISTS',key,threadId) == 0) then
    -- 不存在，直接返回
    return nil
end

-- 存在，value值减1
local count = redis.call('HINCRBY',key,threadId,-1)
-- 如果count大于0，说明仍有业务未执行完
if (count > 0) then
    redis.call('expire',key,releaseTime)
    return nil
else
    redis.call('del',key)
    return nil
end