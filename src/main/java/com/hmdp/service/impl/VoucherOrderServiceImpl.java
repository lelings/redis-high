package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 执行秒杀业务的lua脚本
     */
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }


    private static final ExecutorService SECKILL_ORDER_POOL = Executors.newSingleThreadExecutor();

    /**
     * 事先获取代理对象
     */
    private IVoucherOrderService proxy;

    /**
     * 在项目启动时开启异步线程监听消息队列，处理消息
     */
    @PostConstruct
    public void init() {
        SECKILL_ORDER_POOL.submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        // 从消息队列中读取消息
                        List<MapRecord<String, Object, Object>> recordList = stringRedisTemplate.opsForStream().read(
                                Consumer.from("g1", "c1"),
                                StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                                StreamOffset.create("stream.orders", ReadOffset.lastConsumed())
                        );
                        if (recordList == null || recordList.isEmpty()) {
                            continue;
                        }
                        // 解析消息
                        MapRecord<String, Object, Object> mapRecord = recordList.get(0);
                        Map<Object, Object> map = mapRecord.getValue();
                        VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(map, new VoucherOrder(), true);
                        // 处理订单
                        handleVoucherOrder(voucherOrder);
                        stringRedisTemplate.opsForStream().acknowledge("stream.orders","g1",mapRecord.getId());
                    } catch (Exception e) {
                        log.error("处理stream消息队列订单失败");
                        handlePendingList();
                    }

                }
            }
        });
    }

    /**
     * 当出现异常时，从pending-list中取出未确认当消息并处理
     */
    private void handlePendingList() {
        while (true) {
            try {
                List<MapRecord<String, Object, Object>> recordList = stringRedisTemplate.opsForStream().read(
                        Consumer.from("g1", "c1"),
                        StreamReadOptions.empty().count(1),
                        StreamOffset.create("stream.orders", ReadOffset.from("0"))
                );
                if (recordList == null || recordList.isEmpty()) {
                    break;
                }
                MapRecord<String, Object, Object> mapRecord = recordList.get(0);
                Map<Object, Object> map = mapRecord.getValue();
                VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(map, new VoucherOrder(), true);
                handleVoucherOrder(voucherOrder);
                stringRedisTemplate.opsForStream().acknowledge("stream.orders","g1",mapRecord.getId());
            } catch (Exception e) {
                log.error("处理pending-list失败");
            }

        }
    }

//    private BlockingQueue<VoucherOrder> orderTasks = new LinkedBlockingQueue<>(1024*1024);
//    @PostConstruct
//    public void init() {
//        SECKILL_ORDER_POOL.submit(new Runnable() {
//            @Override
//            public void run() {
//                while (true) {
//                    VoucherOrder voucherOrder = null;
//                    try {
//                        voucherOrder = orderTasks.take();
//                    } catch (Exception e) {
//                        log.error("队列处理订单失败");
//                    }
//                    handleVoucherOrder(voucherOrder);
//                }
//            }
//        });
//    }

    /**
     * 处理秒杀订单
     * @param voucherOrder
     */
    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        SimpleRedisLock lock = new SimpleRedisLock(stringRedisTemplate, "order:" + userId);
        boolean isLock = lock.tryLock(5);
        if (!isLock) {
            log.error("下单失败");
        }
        // 创建动态代理防止事务失效
        try {
            proxy.createVoucherOrder(voucherOrder);
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 处理秒杀业务的入口
     * @param voucherId
     * @return
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        long id = redisIdWorker.nextId("order");
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString(),
                String.valueOf(id)
        );
        int r = result.intValue();
        if (r != 0) {
            return Result.fail(r == 1 ? "库存不足" : "只能下一单");
        }

        proxy = (IVoucherOrderService) AopContext.currentProxy();
        return Result.ok(id);
    }

//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        LocalDateTime now = LocalDateTime.now();
//        if (now.isBefore(voucher.getBeginTime())) {
//            return Result.fail("秒杀未开始");
//        }
//        if (now.isAfter(voucher.getEndTime())) {
//            return Result.fail("秒杀已结束");
//        }
//
//        if (voucher.getStock() < 1) {
//            return Result.fail("库存不足");
//        }
//        Long userId = UserHolder.getUser().getId();
//        SimpleRedisLock lock = new SimpleRedisLock(stringRedisTemplate, "order:" + userId);
//        boolean isLock = lock.tryLock(5);
//        if (!isLock) {
//            return Result.fail("一个用户不允许重复下单");
//        }
//        // 创建动态代理防止事务失效
//        try {
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createOrder(voucherId);
//        } catch (IllegalStateException e) {
//            throw new RuntimeException(e);
//        } finally {
//            lock.unlock();
//        }
//    }

//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        Long userId = UserHolder.getUser().getId();
//        Long result = stringRedisTemplate.execute(
//                SECKILL_SCRIPT,
//                Collections.emptyList(),
//                voucherId.toString(),
//                userId.toString()
//        );
//        int r = result.intValue();
//        if (r != 0) {
//            return Result.fail(r == 1 ? "库存不足" : "只能下一单");
//        }
//
//        VoucherOrder voucherOrder = new VoucherOrder();
//        long id = redisIdWorker.nextId("order");
//        voucherOrder.setId(id);
//        voucherOrder.setVoucherId(voucherId);
//        voucherOrder.setUserId(userId);
//        orderTasks.add(voucherOrder);
//        proxy = (IVoucherOrderService) AopContext.currentProxy();
//        return Result.ok(id);
//    }

    /**
     * 更新数据库中库存信息，创建订单信息
     * @param voucherOrder
     */
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();
        int count = query().eq("voucher_id", voucherId).eq("user_id", userId).count();
        if (count > 0) {
            log.error("处理订单失败");
            return;
        }
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock",0)
                .update();
        if (!success) {
            log.error("处理订单失败");
            return;
        }
        save(voucherOrder);
    }
}
