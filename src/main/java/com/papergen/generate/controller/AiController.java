package com.papergen.generate.controller;

import com.papergen.generate.domain.model.R;
import com.papergen.generate.domain.params.AiModelConfigParam;
import com.papergen.generate.domain.vo.AiChatMessageVO;
import com.papergen.generate.domain.vo.AiChatVO;
import com.papergen.generate.domain.vo.AiModelConfigVO;
import com.papergen.generate.service.AiPaperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * AI 导卷：上传任意格式文件，对话生成试卷
 */
@Tag(name = "AI 导卷", description = "文件转试卷 JSON 并生成试卷")
@RestController
@RequestMapping("/v1/ai")
public class AiController {

    @Autowired
    private AiPaperService aiPaperService;

    @Operation(summary = "AI 对话生成试卷", description = "携带文件时优先解析文件内容并生成试卷，返回 paperId 后可跳转答题")
    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<AiChatVO> chat(@RequestParam("sessionId") String sessionId,
                            @RequestParam(value = "message", required = false) String message,
                            @RequestParam(value = "file", required = false) MultipartFile file) {
        return R.ok(aiPaperService.chat(sessionId, message, file));
    }

    @Operation(summary = "对话记忆", description = "返回指定会话最近的对话消息")
    @GetMapping("/messages")
    public R<List<AiChatMessageVO>> messages(@RequestParam("sessionId") String sessionId) {
        return R.ok(aiPaperService.history(sessionId));
    }

    @Operation(summary = "清空对话记忆")
    @DeleteMapping("/messages")
    public R<Void> clearMessages(@RequestParam("sessionId") String sessionId) {
        aiPaperService.clearHistory(sessionId);
        return R.ok();
    }

    @Operation(summary = "模型配置", description = "返回当前用户配置的模型地址、模型名与是否已设置密钥")
    @GetMapping("/config")
    public R<AiModelConfigVO> config() {
        return R.ok(aiPaperService.getConfig());
    }

    @Operation(summary = "保存模型配置", description = "apiKey 留空表示沿用已保存的密钥")
    @PutMapping("/config")
    public R<AiModelConfigVO> saveConfig(@RequestBody AiModelConfigParam param) {
        return R.ok(aiPaperService.saveConfig(param));
    }
}
