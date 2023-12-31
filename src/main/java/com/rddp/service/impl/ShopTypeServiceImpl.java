package com.rddp.service.impl;

import com.rddp.entity.ShopType;
import com.rddp.mapper.ShopTypeMapper;
import com.rddp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

//    @Override
//    public Result listAll() {
//        String key = RedisConstants.CACHE_SHOP_TYPE_KEY;
//        List<String> list = stringRedisTemplate.opsForList().range(key, 0, -1);
//        List<ShopType> shopList = new ArrayList<>();
//        if (list != null) {
//            for (String shop : list) {
//                ShopType shopType = JSONUtil.toBean(shop, ShopType.class);
//                shopList.add(shopType);
//            }
//            return Result.ok(shopList);
//        }
//        shopList = query().orderByAsc("sort").list();
//        if (shopList == null) {
//            return Result.fail("店铺不存在");
//        }
//
//        stringRedisTemplate.opsForList().set();
//
//
//    }
}
