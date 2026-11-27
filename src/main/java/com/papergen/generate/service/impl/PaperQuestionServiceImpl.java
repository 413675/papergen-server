package com.papergen.generate.service.impl;

import com.github.yulichang.base.MPJBaseServiceImpl;
import com.papergen.generate.domain.entity.PaperQuestion;
import com.papergen.generate.service.PaperQuestionService;
import com.papergen.generate.mapper.PaperQuestionMapper;
import org.springframework.stereotype.Service;

/**
* @author 20413
* @description 针对表【paper_question(试卷题目关联表)】的数据库操作Service实现
* @createDate 2026-11-27 14:11:20
*/
@Service
public class PaperQuestionServiceImpl extends MPJBaseServiceImpl<PaperQuestionMapper, PaperQuestion>
    implements PaperQuestionService{

}




