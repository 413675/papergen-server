package com.papergen.generate.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户信息")
@Data
public class UserVO {

    @Schema(description = "用户主键")
    private Long sysUserId;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "显示名称")
    private String displayName;

    @Schema(description = "角色：user-普通用户 | teacher-教师 | admin-管理员")
    private String role;
}
