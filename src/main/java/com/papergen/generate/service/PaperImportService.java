package com.papergen.generate.service;

import com.papergen.generate.domain.vo.PaperImportVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 试卷文件导入
 */
public interface PaperImportService {

    /**
     * 解析导入文件，生成草稿试卷、题目与导入任务
     */
    PaperImportVO importPaper(MultipartFile file);
}
