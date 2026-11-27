package com.papergen.generate.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.papergen.generate.service.captcha.SliderCaptchaService;
import com.papergen.generate.constant.HttpStatus;
import com.papergen.generate.constant.UserConstant;
import com.papergen.generate.context.PapergenContext;
import com.papergen.generate.domain.entity.SysUser;
import com.papergen.generate.domain.params.LoginParam;
import com.papergen.generate.domain.params.RegisterParam;
import com.papergen.generate.domain.vo.CaptchaVO;
import com.papergen.generate.domain.vo.LoginVO;
import com.papergen.generate.domain.vo.UserVO;
import com.papergen.generate.exception.BusinessException;
import com.papergen.generate.service.AuthService;
import com.papergen.generate.service.SysUserService;
import com.papergen.generate.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 认证业务实现：注册/登录均先校验滑块验证码，密码使用 BCrypt 存储。
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SliderCaptchaService sliderCaptchaService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public CaptchaVO captcha() {
        return sliderCaptchaService.generate();
    }

    @Override
    public LoginVO login(LoginParam param) {
        checkCaptcha(param.getCaptchaId(), param.getSliderX());

        SysUser user = sysUserService.getOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, param.getUsername()), false);
        if (user == null || !BCrypt.checkpw(param.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!Integer.valueOf(UserConstant.STATUS_ENABLED).equals(user.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "账号已被禁用，请联系管理员");
        }
        return buildLoginVO(user);
    }

    @Override
    public LoginVO register(RegisterParam param) {
        checkCaptcha(param.getCaptchaId(), param.getSliderX());

        boolean exists = sysUserService.count(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, param.getUsername())) > 0;
        if (exists) {
            throw new BusinessException("该账号已被注册");
        }

        SysUser user = new SysUser();
        user.setUsername(param.getUsername());
        user.setPasswordHash(BCrypt.hashpw(param.getPassword(), BCrypt.gensalt()));
        user.setDisplayName(StringUtils.hasText(param.getDisplayName())
                ? param.getDisplayName().trim()
                : param.getUsername());
        user.setRole(UserConstant.DEFAULT_ROLE);
        user.setStatus(UserConstant.STATUS_ENABLED);
        try {
            sysUserService.save(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("该账号已被注册");
        }
        return buildLoginVO(user);
    }

    @Override
    public UserVO currentUser() {
        SysUser user = PapergenContext.get();
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "未登录或登录已过期");
        }
        return toUserVO(user);
    }

    private void checkCaptcha(String captchaId, Integer sliderX) {
        if (!sliderCaptchaService.verify(captchaId, sliderX)) {
            throw new BusinessException("滑块验证失败，请重试");
        }
    }

    private LoginVO buildLoginVO(SysUser user) {
        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.generateToken(user));
        vo.setExpiresIn(jwtUtil.getExpireSeconds());
        vo.setUser(toUserVO(user));
        return vo;
    }

    private UserVO toUserVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setSysUserId(user.getSysUserId());
        vo.setUsername(user.getUsername());
        vo.setDisplayName(user.getDisplayName());
        vo.setRole(user.getRole());
        return vo;
    }
}
