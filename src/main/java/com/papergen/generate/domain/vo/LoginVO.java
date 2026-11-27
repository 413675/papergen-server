package com.papergen.generate.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "登录结果")
@Data
public class LoginVO {

    @Schema(description = "JWT 令牌，前端存入 Cookie")
    private String token;

    @Schema(description = "令牌类型")
    private String tokenType = "Bearer";

    @Schema(description = "有效期（秒）")
    private long expiresIn;

    @Schema(description = "登录用户")
    private UserVO user;
}
