package com.air.aiagent.rag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author WyH524
 * @since 2025/8/1 上午11:11
 * MarkDown文档加载器
 */
@Slf4j
@Component
public class TeenSupportDocumentLoader {

    private final ResourcePatternResolver resourcePatternResolver;

    public TeenSupportDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 加载多篇MarkDown文档
     */
    public List<Document> loadMarkdown() {
        List<Document> allDocumentList = new ArrayList<>();

        try {
            // 尝试从 classpath 加载
            try {
                Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
                if (resources.length > 0) {
                    log.info("从 classpath 加载 Markdown 文档，数量：{}", resources.length);
                    for (Resource resource : resources) {
                        processResource(resource, allDocumentList);
                    }
                    return allDocumentList;
                }
            } catch (Exception e) {
                log.warn("从 classpath 加载失败，尝试从 src/main/resources 加载: {}", e.getMessage());
            }

            // 尝试从 src/main/resources/document 目录加载
            try {
                File documentDir = new File("src/main/resources/document");
                if (documentDir.exists() && documentDir.isDirectory()) {
                    File[] mdFiles = documentDir.listFiles((dir, name) -> name.endsWith(".md"));
                    if (mdFiles != null && mdFiles.length > 0) {
                        log.info("从 src/main/resources/document 加载 Markdown 文档，数量：{}", mdFiles.length);
                        for (File file : mdFiles) {
                            FileSystemResource resource = new FileSystemResource(file);
                            processResource(resource, allDocumentList);
                        }
                        return allDocumentList;
                    }
                }
            } catch (Exception e) {
                log.warn("从 src/main/resources/document 加载失败: {}", e.getMessage());
            }

            // 尝试从当前目录的 resources/document 加载
            try {
                File documentDir = new File("resources/document");
                if (documentDir.exists() && documentDir.isDirectory()) {
                    File[] mdFiles = documentDir.listFiles((dir, name) -> name.endsWith(".md"));
                    if (mdFiles != null && mdFiles.length > 0) {
                        log.info("从 resources/document 加载 Markdown 文档，数量：{}", mdFiles.length);
                        for (File file : mdFiles) {
                            FileSystemResource resource = new FileSystemResource(file);
                            processResource(resource, allDocumentList);
                        }
                        return allDocumentList;
                    }
                }
            } catch (Exception e) {
                log.warn("从 resources/document 加载失败: {}", e.getMessage());
            }

            // 尝试从 jar 所在目录的 knowledge 文件夹加载（用于部署环境）
            try {
                // 获取当前 jar 文件的路径
                String jarPath = TeenSupportDocumentLoader.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                File jarFile = new File(jarPath);
                File jarDir = jarFile.getParentFile();
                File knowledgeDir = new File(jarDir, "knowledge");
                
                if (knowledgeDir.exists() && knowledgeDir.isDirectory()) {
                    File[] mdFiles = knowledgeDir.listFiles((dir, name) -> name.endsWith(".md"));
                    if (mdFiles != null && mdFiles.length > 0) {
                        log.info("从 jar 所在目录的 knowledge 文件夹加载 Markdown 文档，数量：{}", mdFiles.length);
                        for (File file : mdFiles) {
                            FileSystemResource resource = new FileSystemResource(file);
                            processResource(resource, allDocumentList);
                        }
                        return allDocumentList;
                    }
                }
            } catch (Exception e) {
                log.warn("从 jar 所在目录的 knowledge 文件夹加载失败: {}", e.getMessage());
            }

            log.warn("未找到任何 Markdown 文档");
        } catch (Exception e) {
            log.error("MarkDown 文档加载失败",e);
        }
        return allDocumentList;
    }

    /**
     * 处理单个资源文件
     */
    private void processResource(Resource resource, List<Document> allDocumentList) throws IOException {
        String filename = resource.getFilename();
        log.info("加载文档：{}", filename);

        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(false)
                .withIncludeBlockquote(false)
                .withAdditionalMetadata("filename", filename)
                .build();

        MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
        List<Document> documents = reader.get();

        allDocumentList.addAll(documents);
        log.info("文档 {} 处理完成，生成 {} 个文档", filename, documents.size());
    }
}
