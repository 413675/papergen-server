package com.papergen.generate.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.papergen.generate.context.PapergenContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 审计字段自动填充。
 * 实体字段需标注 @TableField(fill = ...) 才会进入这里：
 * createBy/createTime 插入时填充，updateBy/updateTime 插入和更新时填充。
 * 未登录（上下文为空）时不覆盖 createBy/updateBy。
 */
@Component
public class AutoHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Long userId = PapergenContext.getUserId();
        if (userId != null) {
            this.strictInsertFill(metaObject, "createBy", Long.class, userId);
            this.strictInsertFill(metaObject, "updateBy", Long.class, userId);
        }
        Date now = new Date();
        this.strictInsertFill(metaObject, "createTime", Date.class, now);
        this.strictInsertFill(metaObject, "updateTime", Date.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Long userId = PapergenContext.getUserId();
        if (userId != null) {
            this.strictUpdateFill(metaObject, "updateBy", Long.class, userId);
        }
        this.strictUpdateFill(metaObject, "updateTime", Date.class, new Date());
    }
}
