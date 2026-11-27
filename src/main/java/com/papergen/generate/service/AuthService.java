package com.papergen.generate.service;

import com.papergen.generate.domain.params.LoginParam;
import com.papergen.generate.domain.params.RegisterParam;
import com.papergen.generate.domain.vo.CaptchaVO;
import com.papergen.generate.domain.vo.LoginVO;
import com.papergen.generate.domain.vo.UserVO;

/**
 * 认证相关业务
 */
public interface AuthService {

    /**
     * 生成滑块验证码
     */
    CaptchaVO captcha();

    /**
     * 登录并签发令牌
     */
    LoginVO login(LoginParam param);

    /**
     * 注册并直接登录，角色默认 user
     */
    LoginVO register(RegisterParam param);

    /**
     * 获取当前登录用户
     */
    UserVO currentUser();
}
