package com.papergen.config;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Swagger2的接口配置
 * 
 * @author ruoyi
 */
@Configuration
public class SwaggerConfig
{

    
    /**
     * 自定义的 OpenAPI 对象
     */
    @Bean
    public OpenAPI customOpenApi()
    {
        return new OpenAPI().components(new Components()
            // 设置认证的请求头
            .addSecuritySchemes("apikey", securityScheme()))
            .addSecurityItem(new SecurityRequirement().addList("apikey"))
            .info(getApiInfo());
    }

    @Bean
    public GroupedOpenApi defaultApi()
    {
        return GroupedOpenApi.builder()
            .group("papergen")
            .displayName("试卷助手模块")
            .pathsToMatch("/**")
            .packagesToScan("com.papergen.controller")
            .build();
    }

    /**
     * 将 Springdoc 和 Knife4j 的配置作为 Java 默认属性加载，避免放在 application.yml 中。
     */
    public static Map<String, Object> defaultProperties()
    {
        return Map.ofEntries(
            Map.entry("springdoc.api-docs.path", "/v3/api-docs"),
            Map.entry("springdoc.swagger-ui.enabled", "true"),
            Map.entry("springdoc.swagger-ui.path", "/swagger-ui.html"),
            Map.entry("springdoc.swagger-ui.tags-sorter", "alpha"),
            Map.entry("springdoc.swagger-ui.config-url", "/ms_stu_${ruoyi.project-name}/v3/api-docs/default"),
            Map.entry("springdoc.swagger-ui.url", "/ms_stu_${ruoyi.project-name}/v3/api-docs"),
            Map.entry("knife4j.enable", "true"),
            Map.entry("knife4j.production", "false"),
            Map.entry("knife4j.basic.enable", "false"),
            Map.entry("knife4j.basic.username", "ruoyi"),
            Map.entry("knife4j.basic.password", "123456"),
            Map.entry("knife4j.setting.swagger-model-name", "实体类列表")
        );
    }
    
    @Bean
    public SecurityScheme securityScheme()
    {
        return new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .name("Authorization")
            .in(SecurityScheme.In.HEADER)
            .scheme("Bearer");
    }
    
    /**
     * 添加摘要信息
     */
    public Info getApiInfo()
    {
        return new Info()
            // 设置标题
            .title("试卷助手接口文档")
            // 描述
            .description("试卷助手 API 接口文档")
            // 作者信息
            .contact(new Contact().name("试卷助手"))
            // 版本
            .version("版本号:1.0.0");
    }

    /**
     * Knife4j 需要 operation 级别的安全要求才能为受保护接口显示 Authorization 输入框。
     * 显式声明为空 SecurityRequirements 的公开接口不追加认证要求。
     */
    private OpenApiCustomizer authRequiredCustomizer()
    {
        return openApi ->
        {
            SecurityRequirement requirement = new SecurityRequirement().addList("apikey");
            openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation ->
            {
                if (operation.getSecurity() == null)
                {
                    operation.addSecurityItem(requirement);
                }
            }));
            openApi.addSecurityItem(requirement);
        };
    }
}
