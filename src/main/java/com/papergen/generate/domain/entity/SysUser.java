package com.papergen.generate.domain.entity;

import com.papergen.generate.domain.model.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户表
 * @TableName sys_user
 */
@Schema(description = "用户表")
@TableName(value ="sys_user")
@Data
public class SysUser extends BaseModel {
    /**
     * 主键
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long sysUserId;

    /**
     * 登录账号
     */
    @Schema(description = "登录账号")
    private String username;

    /**
     * 密码哈希
     */
    @Schema(description = "密码哈希")
    private String passwordHash;

    /**
     * 显示名称
     */
    @Schema(description = "显示名称")
    private String displayName;

    /**
     * teacher-教师 | admin-管理员
     */
    @Schema(description = "teacher-教师 | admin-管理员")
    private String role;

    /**
     * 1-正常 | 0-禁用
     */
    @Schema(description = "1-正常 | 0-禁用")
    private Integer status;
}