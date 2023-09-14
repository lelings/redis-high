package com.rddp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rddp.dto.LoginFormDTO;
import com.rddp.dto.Result;
import com.rddp.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    Result sendCode(String code, HttpSession httpSession);

    Result login(LoginFormDTO loginForm, HttpSession session);
}
