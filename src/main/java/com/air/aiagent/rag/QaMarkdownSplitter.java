package com.air.aiagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 问答格式Markdown文档切分器
 * 专门针对一问一答格式的Markdown文档进行优化切分
 * 确保每个问答对作为一个完整的文档块，不被切分
 * @author AI Agent
 * @since 2025/4/30
 */
@Component
public class QaMarkdownSplitter {

    /**
     * 问答标题的正则模式
     * 匹配 #### 开头的标题行
     */
    private static final Pattern QA_TITLE_PATTERN = Pattern.compile("^(####\\s+.+)$", Pattern.MULTILINE);

    /**
     * 切分问答格式的Markdown文档
     * 将每个问答对作为一个独立的Document
     *
     * @param documents 原始文档列表
     * @return 切分后的文档列表，每个问答对为一个文档
     */
    public List<Document> splitQaDocuments(List<Document> documents) {
        List<Document> result = new ArrayList<>();

        for (Document doc : documents) {
            String content = doc.getText();
            Map<String, Object> metadata = new HashMap<>(doc.getMetadata());

            // 按问答标题切分
            List<Document> qaDocs = splitByQaTitle(content, metadata);
            result.addAll(qaDocs);
        }

        return result;
    }

    /**
     * 按问答标题切分内容
     * 每个以 #### 开头的标题及其后续内容作为一个独立的文档块
     *
     * @param content 文档内容
     * @param baseMetadata 基础元数据
     * @return 切分后的文档列表
     */
    private List<Document> splitByQaTitle(String content, Map<String, Object> baseMetadata) {
        List<Document> documents = new ArrayList<>();

        if (content == null || content.trim().isEmpty()) {
            return documents;
        }

        // 匹配所有问答标题的位置
        Matcher matcher = QA_TITLE_PATTERN.matcher(content);
        List<Integer> titlePositions = new ArrayList<>();
        List<String> titles = new ArrayList<>();

        while (matcher.find()) {
            titlePositions.add(matcher.start());
            titles.add(matcher.group(1).trim());
        }

        // 如果没有找到问答标题，将整个内容作为一个文档
        if (titlePositions.isEmpty()) {
            Document doc = createDocument(content.trim(), baseMetadata, "article");
            documents.add(doc);
            return documents;
        }

        // 按标题位置切分内容
        for (int i = 0; i < titlePositions.size(); i++) {
            int startPos = titlePositions.get(i);
            int endPos = (i + 1 < titlePositions.size()) ? titlePositions.get(i + 1) : content.length();

            String qaContent = content.substring(startPos, endPos).trim();

            if (!qaContent.isEmpty()) {
                // 提取问题标题（去掉 #### 前缀）
                String questionTitle = titles.get(i).replaceAll("^####\\s*", "");

                // 创建新的元数据，包含问题标题
                Map<String, Object> qaMetadata = new HashMap<>(baseMetadata);
                qaMetadata.put("question_title", questionTitle);
                qaMetadata.put("chunk_type", "qa_pair");

                Document doc = createDocument(qaContent, qaMetadata, "qa_pair");
                documents.add(doc);
            }
        }

        return documents;
    }

    /**
     * 创建文档对象
     *
     * @param content 文档内容
     * @param metadata 元数据
     * @param chunkType 切分类型
     * @return Document对象
     */
    private Document createDocument(String content, Map<String, Object> metadata, String chunkType) {
        metadata.put("chunk_type", chunkType);
        return new Document(content, metadata);
    }

    /**
     * 切分文章类型的Markdown文档
     * 当文档不是问答格式时，按段落进行切分
     *
     * @param documents 原始文档列表
     * @param maxChunkSize 最大块大小（字符数）
     * @return 切分后的文档列表
     */
    public List<Document> splitArticleDocuments(List<Document> documents, int maxChunkSize) {
        List<Document> result = new ArrayList<>();

        for (Document doc : documents) {
            String content = doc.getText();
            Map<String, Object> metadata = new HashMap<>(doc.getMetadata());

            // 如果内容较短，直接保留
            if (content.length() <= maxChunkSize) {
                metadata.put("chunk_type", "article");
                result.add(new Document(content, metadata));
                continue;
            }

            // 按段落切分
            List<Document> articleDocs = splitByParagraph(content, metadata, maxChunkSize);
            result.addAll(articleDocs);
        }

        return result;
    }

    /**
     * 按段落切分文章
     *
     * @param content 文档内容
     * @param baseMetadata 基础元数据
     * @param maxChunkSize 最大块大小
     * @return 切分后的文档列表
     */
    private List<Document> splitByParagraph(String content, Map<String, Object> baseMetadata, int maxChunkSize) {
        List<Document> documents = new ArrayList<>();

        // 按双换行符分割段落
        String[] paragraphs = content.split("\\n\\n+");

        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) {
                continue;
            }

            // 如果当前块加上新段落不超过最大大小，则添加
            if (currentChunk.length() + paragraph.length() + 2 <= maxChunkSize) {
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(paragraph);
            } else {
                // 保存当前块
                if (currentChunk.length() > 0) {
                    Map<String, Object> chunkMetadata = new HashMap<>(baseMetadata);
                    chunkMetadata.put("chunk_type", "article_chunk");
                    chunkMetadata.put("chunk_index", chunkIndex++);
                    documents.add(new Document(currentChunk.toString(), chunkMetadata));
                }

                // 开始新块
                currentChunk = new StringBuilder(paragraph);
            }
        }

        // 保存最后一个块
        if (currentChunk.length() > 0) {
            Map<String, Object> chunkMetadata = new HashMap<>(baseMetadata);
            chunkMetadata.put("chunk_type", "article_chunk");
            chunkMetadata.put("chunk_index", chunkIndex);
            documents.add(new Document(currentChunk.toString(), chunkMetadata));
        }

        return documents;
    }

    /**
     * 智能切分：自动识别文档类型并选择合适的切分策略
     * 如果文档包含 #### 标题，则按问答格式切分
     * 否则按文章格式切分
     *
     * @param documents 原始文档列表
     * @return 切分后的文档列表
     */
    public List<Document> smartSplit(List<Document> documents) {
        List<Document> result = new ArrayList<>();

        for (Document doc : documents) {
            String content = doc.getText();

            // 检测是否为问答格式
            boolean isQaFormat = QA_TITLE_PATTERN.matcher(content).find();

            if (isQaFormat) {
                // 问答格式：按问答对切分
                List<Document> qaDocs = splitByQaTitle(content, new HashMap<>(doc.getMetadata()));
                result.addAll(qaDocs);
            } else {
                // 文章格式：保持原样或按段落切分
                Map<String, Object> metadata = new HashMap<>(doc.getMetadata());
                metadata.put("chunk_type", "article");
                result.add(new Document(content, metadata));
            }
        }

        return result;
    }
}
