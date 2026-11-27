package com.papergen.generate.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "滑块验证码")
@Data
public class CaptchaVO {

    @Schema(description = "验证码标识，登录/注册时原样回传")
    private String captchaId;

    @Schema(description = "背景图（data:image/png;base64 格式，含缺口）")
    private String backgroundImage;

    @Schema(description = "滑块图（data:image/png;base64 格式，透明背景）")
    private String sliderImage;

    @Schema(description = "背景图宽度")
    private int width;

    @Schema(description = "背景图高度")
    private int height;

    @Schema(description = "滑块宽度")
    private int sliderWidth;

    @Schema(description = "滑块高度")
    private int sliderHeight;

    @Schema(description = "滑块初始纵坐标")
    private int sliderY;
}
