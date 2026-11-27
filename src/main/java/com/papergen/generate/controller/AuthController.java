package com.papergen.generate.controller;

import com.papergen.generate.domain.model.R;
import com.papergen.generate.domain.params.LoginParam;
import com.papergen.generate.domain.params.RegisterParam;
import com.papergen.generate.domain.vo.CaptchaVO;
import com.papergen.generate.domain.vo.LoginVO;
import com.papergen.generate.domain.vo.UserVO;
import com.papergen.generate.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：验证码、注册、登录、当前用户、退出
 */
@Tag(name = "认证", description = "滑块验证码、注册、登录、当前用户")
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "获取滑块验证码")
    @GetMapping("/captcha")
    public R<CaptchaVO> captcha() {
        return R.ok(authService.captcha());
    }

    @Operation(summary = "注册（角色默认 user）")
    @PostMapping("/register")
    public R<LoginVO> register(@Valid @RequestBody RegisterParam param) {
        return R.ok(authService.register(param), "注册成功");
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginParam param) {
        return R.ok(authService.login(param), "登录成功");
    }

    @Operation(summary = "获取当前登录用户")
    @GetMapping("/me")
    public R<UserVO> me() {
        return R.ok(authService.currentUser());
    }

    @Operation(summary = "退出登录（前端清除 Cookie 即可）")
    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok(null, "退出成功");
    }
}
