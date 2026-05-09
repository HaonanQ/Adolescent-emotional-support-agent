package com.air.aiagent.rag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

/**
 * @author Qiuhaonan
 * @since 2025/8/1 下午3:27
 * 向量数据库配置（初始化基于内存的向量数据库Bean）
 */
//@Configuration
@Slf4j
public class TeenSupportVectorStoreConfig {

    @Resource
    private TeenSupportDocumentLoader teenSupportDocumentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    /**
     * 阿里云嵌入模型批量大小限制
     */
    private static final int BATCH_SIZE = 10;

    @Bean
    VectorStore teenSupportVectorStore(EmbeddingModel embeddingModel){
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();

        List<Document> list = teenSupportDocumentLoader.loadMarkdown();

        // 使用智能切分，自动识别问答格式或文章格式
        List<Document> splitCustomizedList = myTokenTextSplitter.splitSmart(list);

        // 分批添加到向量存储（阿里云嵌入模型限制每批最多10个）
        int totalDocs = splitCustomizedList.size();
        for (int i = 0; i < totalDocs; i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, totalDocs);
            List<Document> batch = splitCustomizedList.subList(i, endIndex);
            simpleVectorStore.add(batch);
            log.info("已添加 {}/{} 个文档块到向量存储", endIndex, totalDocs);
        }

        return simpleVectorStore;
    }
}
