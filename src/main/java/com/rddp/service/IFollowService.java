package com.rddp.service;

import com.rddp.dto.Result;
import com.rddp.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {

    Result follow(Long id, boolean followed);

    Result isFollow(Long id);

    Result common(Long id);
}
