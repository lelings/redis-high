package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Override
    public Result follow(Long id, boolean followed) {
        Long userId = UserHolder.getUser().getId();
        if (followed) {
            Follow follow = new Follow();
            follow.setFollowUserId(id);
            follow.setUserId(userId);
            save(follow);
        } else {
            remove(new UpdateWrapper<Follow>().eq("user_id",userId).eq("follow_user_id",id));
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long id) {
        Long userId = UserHolder.getUser().getId();
        Integer count = query().eq("user_id", id).eq("follow_user_id", id).count();
        return Result.ok(count > 0);
    }
}
