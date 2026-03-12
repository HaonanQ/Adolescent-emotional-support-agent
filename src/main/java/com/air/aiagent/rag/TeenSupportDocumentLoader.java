package com.air.aiagent.rag;

import com.air.aiagent.domain.entity.KnowledgeDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 知识库文档加载器
 * 支持按知识库分文件夹加载文档
 * 文件结构：knowledge/{知识库名称}/{文档文件}
 * @author Qiuhaonan
 * @since 2025/8/1 上午11:11
 */
@Slf4j
@Component
public class TeenSupportDocumentLoader {

    /**
     * 加载指定知识库的文档
     * 从 knowledge/{knowledgeBaseName}/ 目录加载所有支持的文档
     *
     * @param knowledgeBaseName 知识库名称（文件夹名）
     * @return 文档列表
     */
    public List<Document> loadDocumentsByKnowledgeBase(String knowledgeBaseName) {
        List<Document> allDocumentList = new ArrayList<>();

        if (knowledgeBaseName == null || knowledgeBaseName.trim().isEmpty()) {
            log.warn("知识库名称为空，无法加载文档");
            return allDocumentList;
        }

        try {
            // 获取当前执行目录（Jar所在目录）
            String currentDir = System.getProperty("user.dir");
            File jarDir = new File(currentDir);
            log.info("当前执行目录：{}，准备加载知识库 [{}] 的文档", jarDir.getAbsolutePath(), knowledgeBaseName);

            // 构建知识库目录路径：knowledge/{知识库名称}/
            File knowledgeBaseDir = new File(jarDir, "knowledge" + File.separator + knowledgeBaseName);
            log.info("知识库文档目录：{}，是否存在：{}",
                    knowledgeBaseDir.getAbsolutePath(), knowledgeBaseDir.exists());

            if (!knowledgeBaseDir.exists() || !knowledgeBaseDir.isDirectory()) {
                log.warn("知识库 [{}] 的文档目录不存在，路径：{}", knowledgeBaseName, knowledgeBaseDir.getAbsolutePath());
                return allDocumentList;
            }

            // 加载目录下所有支持的文档文件
            File[] files = knowledgeBaseDir.listFiles((dir, name) -> isSupportedFile(name));

            if (files == null || files.length == 0) {
                log.warn("知识库 [{}] 的文档目录为空，路径：{}，目录内容：{}",
                        knowledgeBaseName,
                        knowledgeBaseDir.getAbsolutePath(),
                        Arrays.toString(knowledgeBaseDir.list()));
                return allDocumentList;
            }

            log.info("知识库 [{}] 发现 {} 个文档文件", knowledgeBaseName, files.length);

            for (File file : files) {
                FileSystemResource resource = new FileSystemResource(file);
                processResource(resource, allDocumentList, knowledgeBaseName);
            }

            log.info("知识库 [{}] 文档加载完成，共 {} 个文档块", knowledgeBaseName, allDocumentList.size());

        } catch (Exception e) {
            log.error("加载知识库 [{}] 文档失败", knowledgeBaseName, e);
        }

        return allDocumentList;
    }

    /**
     * 根据知识库文档列表加载实际文档内容
     * 从数据库记录的 file_path 加载文件
     *
     * @param documents 知识库文档列表
     * @return 文档内容列表
     */
    public List<Document> loadDocumentsByFileList(List<KnowledgeDocument> documents) {
        List<Document> allDocumentList = new ArrayList<>();

        if (documents == null || documents.isEmpty()) {
            log.warn("文档列表为空");
            return allDocumentList;
        }

        for (KnowledgeDocument doc : documents) {
            String filePath = doc.getFilePath();
            if (filePath == null || filePath.trim().isEmpty()) {
                log.warn("文档 [{}] 的文件路径为空，跳过", doc.getFileName());
                continue;
            }

            try {
                File file = new File(filePath);
                if (!file.exists()) {
                    log.warn("文档文件不存在：{}，尝试从相对路径加载", filePath);
                    // 尝试从 knowledge 目录加载
                    String currentDir = System.getProperty("user.dir");
                    file = new File(currentDir, filePath);
                }

                if (!file.exists()) {
                    log.error("文档文件不存在：{}，跳过", filePath);
                    continue;
                }

                FileSystemResource resource = new FileSystemResource(file);
                processResource(resource, allDocumentList, doc.getKnowledgeBaseId().toString());

            } catch (Exception e) {
                log.error("加载文档失败：{}，路径：{}", doc.getFileName(), filePath, e);
            }
        }

        log.info("从文档列表加载完成，共 {} 个文档块", allDocumentList.size());
        return allDocumentList;
    }

    /**
     * 加载默认知识库文档（兼容旧版本）
     * 从 knowledge/ 目录直接加载（不分知识库）
     */
    public List<Document> loadMarkdown() {
        List<Document> allDocumentList = new ArrayList<>();

        try {
            String currentDir = System.getProperty("user.dir");
            File jarDir = new File(currentDir);
            log.info("当前执行目录（Jar所在目录）：{}，加载默认知识库文档", jarDir.getAbsolutePath());

            File knowledgeDir = new File(jarDir, "knowledge");
            log.info("尝试加载默认 knowledge 目录：{}，是否存在：{}",
                    knowledgeDir.getAbsolutePath(), knowledgeDir.exists());

            if (!knowledgeDir.exists() || !knowledgeDir.isDirectory()) {
                log.warn("默认 knowledge 目录不存在，路径：{}", knowledgeDir.getAbsolutePath());
                return allDocumentList;
            }

            // 只加载 knowledge 目录下的文件（不包括子目录）
            File[] files = knowledgeDir.listFiles((dir, name) -> {
                File f = new File(dir, name);
                return f.isFile() && isSupportedFile(name);
            });

            if (files == null || files.length == 0) {
                log.warn("默认 knowledge 目录为空或没有支持的文档文件，目录内容：{}",
                        Arrays.toString(knowledgeDir.list()));
                return allDocumentList;
            }

            log.info("从默认 knowledge 目录加载文档，数量：{}", files.length);

            for (File file : files) {
                FileSystemResource resource = new FileSystemResource(file);
                processResource(resource, allDocumentList, "default");
            }

        } catch (Exception e) {
            log.error("加载默认知识库文档失败", e);
        }

        return allDocumentList;
    }

    /**
     * 判断文件是否为支持的类型
     * 目前支持：Markdown 文件
     * 后续可扩展支持：PDF、Word、TXT 等
     *
     * @param filename 文件名
     * @return 是否支持
     */
    private boolean isSupportedFile(String filename) {
        if (filename == null) return false;
        String lowerName = filename.toLowerCase();
        return lowerName.endsWith(".md") ||
               lowerName.endsWith(".markdown");
        // 后续可添加：
        // lowerName.endsWith(".pdf") ||
        // lowerName.endsWith(".txt") ||
        // lowerName.endsWith(".doc") ||
        // lowerName.endsWith(".docx");
    }

    /**
     * 处理单个资源文件
     *
     * @param resource 资源文件
     * @param allDocumentList 文档列表
     * @param knowledgeBaseName 知识库名称（用于元数据）
     */
    private void processResource(Resource resource, List<Document> allDocumentList, String knowledgeBaseName) throws IOException {
        String filename = resource.getFilename();
        log.info("加载文档：{}，知识库：{}", filename, knowledgeBaseName);

        // 根据文件类型选择不同的读取器
        String lowerName = filename != null ? filename.toLowerCase() : "";

        if (lowerName.endsWith(".md") || lowerName.endsWith(".markdown")) {
            // Markdown 文件
            MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                    .withHorizontalRuleCreateDocument(true)
                    .withIncludeCodeBlock(false)
                    .withIncludeBlockquote(false)
                    .withAdditionalMetadata("filename", filename)
                    .withAdditionalMetadata("knowledge_base", knowledgeBaseName)
                    .build();

            MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
            List<Document> documents = reader.get();
            allDocumentList.addAll(documents);

            log.info("Markdown 文档 {} 处理完成，生成 {} 个文档块", filename, documents.size());
        }
        // 后续可添加其他文件类型的处理
        // else if (lowerName.endsWith(".pdf")) { ... }
        // else if (lowerName.endsWith(".txt")) { ... }
    }
}
