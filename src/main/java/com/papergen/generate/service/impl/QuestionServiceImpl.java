package com.papergen.generate.service.impl;

import com.github.yulichang.base.MPJBaseServiceImpl;
import com.papergen.generate.domain.entity.Question;
import com.papergen.generate.service.QuestionService;
import com.papergen.generate.mapper.QuestionMapper;
import org.springframework.stereotype.Service;

/**
* @author 20413
* @description 针对表【question(题目公共表)】的数据库操作Service实现
* @createDate 2026-11-27 14:11:20
*/
@Service
public class QuestionServiceImpl extends MPJBaseServiceImpl<QuestionMapper, Question>
    implements QuestionService{

}




