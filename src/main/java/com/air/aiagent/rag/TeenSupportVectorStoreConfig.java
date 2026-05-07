package com.air.aiagent.rag;
import jakarta.annotation.Resource;
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
public class TeenSupportVectorStoreConfig {

    @Resource
    private TeenSupportDocumentLoader teenSupportDocumentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Bean
    VectorStore teenSupportVectorStore(EmbeddingModel embeddingModel){
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();

        List<Document> list = teenSupportDocumentLoader.loadMarkdown();

        // 使用智能切分，自动识别问答格式或文章格式
        List<Document> splitCustomizedList = myTokenTextSplitter.splitSmart(list);

        simpleVectorStore.add(splitCustomizedList);

        return simpleVectorStore;
    }
}
