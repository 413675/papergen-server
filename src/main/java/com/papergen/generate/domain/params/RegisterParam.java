package com.papergen.generate.domain.params;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "注册参数")
@Data
public class RegisterParam {

    @Schema(description = "登录账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "teacher01")
    @NotBlank(message = "登录账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$", message = "账号需为4-32位字母、数字或下划线")
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需为6-32位")
    private String password;

    @Schema(description = "显示名称，不填默认与账号一致")
    @Size(max = 64, message = "显示名称不能超过64个字符")
    private String displayName;

    @Schema(description = "滑块验证码标识", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "滑块验证码已失效，请刷新后重试")
    private String captchaId;

    @Schema(description = "滑块水平拖动距离（像素）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请先完成滑块验证")
    @Min(value = 0, message = "滑块位置不合法")
    @Max(value = 1000, message = "滑块位置不合法")
    private Integer sliderX;
}
