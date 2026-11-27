package com.papergen.generate.service.impl;

import com.github.yulichang.base.MPJBaseServiceImpl;
import com.papergen.generate.domain.entity.GenerationTask;
import com.papergen.generate.service.GenerationTaskService;
import com.papergen.generate.mapper.GenerationTaskMapper;
import org.springframework.stereotype.Service;

/**
* @author 20413
* @description 针对表【generation_task(生成任务表)】的数据库操作Service实现
* @createDate 2026-11-27 14:11:20
*/
@Service
public class GenerationTaskServiceImpl extends MPJBaseServiceImpl<GenerationTaskMapper, GenerationTask>
    implements GenerationTaskService{

}




