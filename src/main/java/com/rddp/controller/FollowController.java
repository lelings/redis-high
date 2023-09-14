package com.rddp.controller;


import com.rddp.dto.Result;
import com.rddp.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private IFollowService followService;

    @PutMapping("/{id}/{followed}")
    public Result follow(@PathVariable Long id,@PathVariable boolean followed) {
        return followService.follow(id,followed);
    }

    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable Long id) {
        return followService.isFollow(id);
    }

    @GetMapping("/common/{id}")
    public Result common(@PathVariable Long id) {
        return followService.common(id);
    }



}
