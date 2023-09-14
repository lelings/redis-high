package com.rddp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rddp.dto.Result;
import com.rddp.dto.UserDTO;
import com.rddp.entity.Blog;
import com.rddp.entity.FeedResult;
import com.rddp.entity.Follow;
import com.rddp.entity.User;
import com.rddp.mapper.BlogMapper;
import com.rddp.service.IBlogService;
import com.rddp.service.IFollowService;
import com.rddp.service.IUserService;
import com.rddp.utils.SystemConstants;
import com.rddp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IFollowService followService;

    /**
     * 查询
     * @param current
     * @return
     */
    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach((blog) -> {
            this.setUserByBlog(blog);
            this.setBlogLiked(blog);
                }
        );
        return Result.ok(records);
    }

    @Override
    public Result queryBlogById(Long id) {
        Blog blog = getById(id);
        setUserByBlog(blog);
        setBlogLiked(blog);
        return Result.ok(blog);
    }

    private void setBlogLiked(Blog blog) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return;
        }
        Long userId = user.getId();
        String key = "blog:liked:" + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

    @Override
    public Result likeBlog(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key = "blog:liked:" + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null) {
            boolean success = update().setSql("liked = liked + 1").eq("id", id).update();
            if (success) {
                stringRedisTemplate.opsForZSet().add(key,userId.toString(),System.currentTimeMillis());
            }
        }else {
            boolean success = update().setSql("liked = liked - 1").eq("id", id).update();
            if (success) {
                stringRedisTemplate.opsForZSet().remove(key,userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        String key = "blog:liked:" + id;
        Set<String> set = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (set == null || set.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = set.stream().map(Long::valueOf).collect(Collectors.toList());
        String idsStr = StrUtil.join(",", ids);

        List<UserDTO> userDTOS = userService.query().in("id", ids).last("order by field(id," + idsStr + ")").list()
                .stream().map((user -> BeanUtil.copyProperties(user, UserDTO.class)))
                .collect(Collectors.toList());
        return Result.ok(userDTOS);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        Long id = user.getId();
        blog.setUserId(id);
        // 保存探店博文
        boolean success = save(blog);
        if (!success) {
            return Result.fail("发布笔记失败");
        }
        List<Follow> follows = followService.query().eq("follow_user_id", id).list();

        for (Follow follow : follows) {
            String key = "feed:" + follow.getUserId();
            stringRedisTemplate.opsForZSet().add(key,blog.getId().toString(),System.currentTimeMillis());
        }
        return Result.ok(blog);
    }

    /**
     * 滚动查询关注用户博文
     * @param minTime
     * @param offset
     * @return
     */
    @Override
    public Result queryBlogOfFollow(Long minTime, Integer offset) {
        Long userId = UserHolder.getUser().getId();
        String key = "feed:" + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, minTime, offset, 2);
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }
        long nextMinTime = 0L;
        int os = 1;
        List<Long> ids = new ArrayList<>(typedTuples.size());
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            ids.add(Long.valueOf(typedTuple.getValue()));
            long time = typedTuple.getScore().longValue();
            if (time == nextMinTime) {
                os++;
            } else {
                nextMinTime = time;
                os = 1;
            }
        }
        String idsStr = StrUtil.join(",", ids);
        List<Blog> blogs = query().in("id", ids).last("order by field(id," + idsStr + ")").list();
        for (Blog blog : blogs) {
            setBlogLiked(blog);
            setUserByBlog(blog);
        }
        FeedResult feedResult = new FeedResult();
        feedResult.setList(blogs);
        feedResult.setMinTime(nextMinTime);
        feedResult.setOffset(os);
        return Result.ok(feedResult);
    }

    private void setUserByBlog(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
