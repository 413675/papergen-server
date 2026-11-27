package com.papergen.generate.service.impl;

import com.github.yulichang.base.MPJBaseServiceImpl;
import com.papergen.generate.domain.entity.PaperPart;
import com.papergen.generate.service.PaperPartService;
import com.papergen.generate.mapper.PaperPartMapper;
import org.springframework.stereotype.Service;

/**
* @author 20413
* @description 针对表【paper_part(试卷大题（部分）表)】的数据库操作Service实现
* @createDate 2026-11-27 14:11:20
*/
@Service
public class PaperPartServiceImpl extends MPJBaseServiceImpl<PaperPartMapper, PaperPart>
    implements PaperPartService{

}




