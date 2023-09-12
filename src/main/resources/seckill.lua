-- 优惠券id
local voucherId = ARGV[1]
-- 用户id
local userId = ARGV[2]
-- 订单id
local orderId = ARGV[3]

-- redis中的库存key
local stockKey = 'seckill:stock:' .. voucherId
-- redis中的用户key
local voucherKey = 'seckill:order:' .. voucherId

-- 如果库存不够
if (tonumber(redis.call('get',stockKey)) <= 0) then
    return 1
end

-- 如果用户已经下单
if (redis.call('sismember',voucherKey,userId) == 1) then
    return 2
end

-- 库存减1，添加用户
redis.call('incrby',stockKey,-1)
redis.call('sadd',voucherKey,userId)
redis.call('xadd','stream.orders','*','voucherId',voucherId,'userId',userId,'id',orderId)
return 0