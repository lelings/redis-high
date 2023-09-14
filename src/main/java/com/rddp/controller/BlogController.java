package com.rddp.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rddp.dto.Result;
import com.rddp.dto.UserDTO;
import com.rddp.entity.Blog;
import com.rddp.service.IBlogService;
import com.rddp.utils.SystemConstants;
import com.rddp.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;

    /**
     * 添加博文
     * @param blog
     * @return
     */

    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        // 返回id
        return blogService.saveBlog(blog);
    }

    /**
     * 给博文点赞
     * @param id
     * @return
     */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        // 修改点赞数量
        return blogService.likeBlog(id);
    }

    /**
     * 根据id查询某一篇博文的点赞情况
     * @param id
     * @return
     */
    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable Long id) {
        return blogService.queryBlogLikes(id);
    }

    /**
     * 查询自己的博文
     * @param current
     * @return
     */
    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    /**
     * 查询首页博文数据
     * @param current
     * @return
     */
    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }

    /**
     * 根据id查询博文
     * @param id
     * @return
     */
    @GetMapping("{id}")
    public Result queryBlogById(@PathVariable("id") Long id) {
        return blogService.queryBlogById(id);
    }

    /**
     * 拉取关注用户的博文
     * @param minTime
     * @param offset
     * @return
     */
    @GetMapping("/of/follow")
    public Result queryBlogOfFollow(@RequestParam("lastId") Long minTime,
                                    @RequestParam(value = "offset",defaultValue = "0") Integer offset) {
        return blogService.queryBlogOfFollow(minTime,offset);
    }
}
