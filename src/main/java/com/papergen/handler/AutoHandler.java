package com.papergen.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.papergen.context.PapergenContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class AutoHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        try {
            this.setFieldValByName("createBy", PapergenContext.get(), metaObject);
        } catch (Exception e) {
            log.info("异步操作:{}", e.getMessage());
        }
        this.setFieldValByName("createTime", new Date(), metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        try {
            this.setFieldValByName("updateBy", PapergenContext.get(), metaObject);
        } catch (Exception e) {
            log.info("异步操作:{}", e.getMessage());
        }
        this.setFieldValByName("updateTime", new Date(), metaObject);
    }
}
