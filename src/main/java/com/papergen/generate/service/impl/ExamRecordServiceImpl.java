package com.papergen.generate.service.impl;

import com.github.yulichang.base.MPJBaseServiceImpl;
import com.papergen.generate.domain.entity.ExamRecord;
import com.papergen.generate.service.ExamRecordService;
import com.papergen.generate.mapper.ExamRecordMapper;
import org.springframework.stereotype.Service;

/**
* @author 20413
* @description 针对表【exam_record(答卷表（一次作答）)】的数据库操作Service实现
* @createDate 2026-11-27 14:11:20
*/
@Service
public class ExamRecordServiceImpl extends MPJBaseServiceImpl<ExamRecordMapper, ExamRecord>
    implements ExamRecordService{

}




