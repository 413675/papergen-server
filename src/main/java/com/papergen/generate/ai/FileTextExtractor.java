package com.papergen.generate.ai;

import com.papergen.generate.constant.AiConstant;
import com.papergen.generate.exception.BusinessException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * 把用户上传的任意格式文件解析成纯文本，供大模型理解
 */
@Component
public class FileTextExtractor {

    /** 纯文本类后缀，直接按 UTF-8 读取 */
    private static final Set<String> PLAIN_TEXT_EXTENSIONS = Set.of(
            "txt", "md", "markdown", "json", "csv", "xml", "html", "htm", "log");

    public String extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        if (file.getSize() > AiConstant.MAX_FILE_SIZE) {
            throw new BusinessException("文件不能超过 10MB");
        }
        String fileName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "upload";
        String text;
        try {
            text = isPlainText(fileName) ? readPlainText(file) : parseWithTika(file);
        } catch (Exception e) {
            throw new BusinessException("文件解析失败，请尝试转换为 PDF、DOCX 或 TXT 后重试");
        }
        text = normalize(text);
        if (!StringUtils.hasText(text)) {
            throw new BusinessException("未能从文件中解析出文字内容，扫描图片请先转换为可复制文本的文件");
        }
        if (text.length() > AiConstant.MAX_FILE_TEXT_LENGTH) {
            text = text.substring(0, AiConstant.MAX_FILE_TEXT_LENGTH) + "\n（文件内容过长，已截断）";
        }
        return text;
    }

    private boolean isPlainText(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return false;
        }
        return PLAIN_TEXT_EXTENSIONS.contains(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private String readPlainText(MultipartFile file) throws Exception {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        return content.startsWith("\uFEFF") ? content.substring(1) : content;
    }

    private String parseWithTika(MultipartFile file) throws Exception {
        try (InputStream input = file.getInputStream()) {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            parser.parse(input, handler, new Metadata(), new ParseContext());
            return handler.toString();
        }
    }

    /** 统一换行并压缩多余空行 */
    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
