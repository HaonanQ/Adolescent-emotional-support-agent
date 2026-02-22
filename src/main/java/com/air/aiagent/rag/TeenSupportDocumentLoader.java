package com.air.aiagent.rag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
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
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");

            for (Resource resource : resources) {
                String filename = resource.getFilename();

                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", filename)
                        .build();

                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
                List<Document> documents = reader.get();

                allDocumentList.addAll(documents);
            }
        } catch (IOException e) {
            log.error("MarkDown 文档加载失败",e);
        }
        return allDocumentList;
    }
}
