package com.rddp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rddp.dto.LoginFormDTO;
import com.rddp.dto.Result;
import com.rddp.dto.UserDTO;
import com.rddp.entity.User;
import com.rddp.mapper.UserMapper;
import com.rddp.service.IUserService;
import com.rddp.utils.RedisConstants;
import com.rddp.utils.RegexUtils;
import com.rddp.utils.UserHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String RANDOM_USERNAME_PREFIX = "user_";

    /**
     * 发送验证码
     * @param phone
     * @param session
     * @return
     */
    public Result sendCode(String phone, HttpSession session) {
        if(RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY+phone,code,
                RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);


        log.debug("发送短信验证码成功："+code);
        return Result.ok();
    }

    /**
     * 登陆功能
     * @param loginForm
     * @param session
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }

        String cacheCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY+phone);
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.equals(code)) {
            return Result.fail("验证码错误");
        }

        User user = query().eq("phone",phone).one();

        if (user == null) {
            user = createUserWithPhone(phone);
        }

        // 将user信息存储到redis中
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user,userDTO);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true)
                        .setFieldValueEditor((filedName,filedValue)->filedValue.toString()));
        String token = UUID.randomUUID().toString(true);

        stringRedisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER_KEY+token,userMap);
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY+token,
                Duration.ofSeconds(RedisConstants.LOGIN_USER_TTL));
        return Result.ok(token);
    }

    @Override
    public Result sign() {
        Long userId = UserHolder.getUser().getId();
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = RedisConstants.USER_SIGN_KEY + userId + keySuffix;
        int dayOfMonth = now.getDayOfMonth();
        Boolean success = stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        if (BooleanUtil.isFalse(success)) {
            return Result.fail("已经签过到了");
        }
        return Result.ok();
    }

    @Override
    public Result singCount() {
        Long userId = UserHolder.getUser().getId();
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = RedisConstants.USER_SIGN_KEY + userId + keySuffix;
        int dayOfMonth = now.getDayOfMonth();
        List<Long> result = stringRedisTemplate.opsForValue().bitField(key, BitFieldSubCommands
                .create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));

        if (result == null || result.isEmpty()) {
            return Result.ok(0);
        }
        Long signDays = result.get(0);
        if (signDays == null || signDays == 0) {
            return Result.ok(0);
        }
        int count = 0;
        while (true) {
            if ((signDays & 1) == 0) {
                break;
            }else {
                count ++ ;
            }
            signDays >>>= 1;
        }
        return Result.ok(count);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        user.setNickName(RANDOM_USERNAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }

}
