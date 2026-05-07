package com.air.aiagent.tools;

import cn.hutool.core.lang.UUID;
import com.air.aiagent.context.UserContext;
import com.air.aiagent.domain.entity.*;
import com.air.aiagent.manage.CosManager;
import com.air.aiagent.service.UserFileService;
import com.air.aiagent.service.impl.AsyncTaskService;
import com.air.aiagent.service.impl.ChatMessageService;
import com.air.aiagent.service.impl.ChatSessionService;
import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.ListItem;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF生成工具 - 支持Markdown格式解析和图片插入
 * 
 * 功能特性：
 * - 自动解析Markdown格式内容
 * - 支持多级标题 (#, ##, ###)
 * - 支持有序列表和无序列表
 * - 支持粗体(**text**)和斜体(*text*)
 * - 支持引用块(> text)
 * - 支持分隔线(---)
 * - 支持图片插入 ![描述](URL)
 * - 自动处理表情符号
 * - 云端存储集成(MinIO)
 * 
 * @author AI Assistant
 */
@Slf4j
@Component
public class PDFGenerationTool {

    @Resource
    private CosManager cosManager;

    @Resource
    private AsyncTaskService asyncTaskService;

    @Resource
    private UserFileService fileService;
    @Resource
    private ChatSessionService chatSessionService;
    @Resource
    private ChatMessageService chatMessageService;

    /**
     * 图片最大宽度（像素），超过此宽度会自动缩放
     */
    private static final float MAX_IMAGE_WIDTH = 450f;

    /**
     * 图片最大高度（像素），超过此高度会自动缩放
     */
    private static final float MAX_IMAGE_HEIGHT = 400f;

    /**
     * 图片下载超时时间（毫秒）
     */
    private static final int DOWNLOAD_TIMEOUT = 15000;

    @Tool(description = """
            Generate a beautifully formatted PDF from Markdown content.

            Supported Markdown syntax:
            - Headers: # H1, ## H2, ### H3
            - Bold: **text**
            - Italic: *text*
            - Bullet lists: - item or * item
            - Numbered lists: 1. item
            - Quotes: > quote
            - Dividers: ---
            - Images: ![description](image_url)
            - Emojis: 😊 💕 🎉

            Example:
            # Title
            ## Section
            - Point 1
            - Point 2
            **Important** content here!
            ![Beautiful Image](https://example.com/image.jpg)
            """)
    public String generatePDFToCOS(
            @ToolParam(description = "PDF file name using Chinese (e.g., 报告.pdf)") String fileName,
            @ToolParam(description = "Content in Markdown format") String content,
            @ToolParam(description = "User ID for file storage") String userId,
            @ToolParam(description = "Current session ID for message association") String sessionId) throws IOException {

        String safeUserId = (userId != null && !userId.isEmpty()) ? userId : UserContext.getSafeUserId();
        String safeSessionId = (sessionId != null && !sessionId.isEmpty()) ? sessionId : UserContext.getSafeSessionId();
        File pdfFile = File.createTempFile(fileName, ".pdf");

        String filePrefix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String filePath = "public/pdf/" + safeUserId + "/" + filePrefix + "_" + fileName;

        // 用于存储下载的临时图片文件，最后统一清理
        List<Path> tempImageFiles = new ArrayList<>();

        try {
            // 准备 PDF 写入器（写到临时文件）
            try (PdfWriter writer = new PdfWriter(pdfFile.getAbsolutePath());
                    PdfDocument pdf = new PdfDocument(writer);
                    Document document = new Document(pdf)) {

                // 1. 清洗内容
                String cleanedContent = cleanContent(content);
                boolean hasEmoji = containsEmoji(cleanedContent);

                // 2. 加载字体（Regular / Bold / Emoji）
                PdfFont mainFont;
                PdfFont boldFont = null;
                PdfFont emojiFont = null;

                // load regular
                try {
                    // 尝试从类路径加载字体文件
                    InputStream baseIn = getClass().getClassLoader().getResourceAsStream("fonts/NotoSansCJKsc-Regular.otf");
                    if (baseIn != null) {
                        try {
                            byte[] baseBytes = baseIn.readAllBytes();
                            FontProgram fontProgram = FontProgramFactory.createFont(baseBytes);
                            mainFont = PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H,
                                    PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
                            log.info("✓ Base font loaded from classpath");
                        } finally {
                            baseIn.close();
                        }
                    } else {
                        // 回退到内置字体（不会支持中文/emoji）
                        log.warn("⚠ Base font not found in classpath, fallback to Helvetica");
                        mainFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                    }
                } catch (Exception e) {
                    // 回退到内置字体（不会支持中文/emoji）
                    log.warn("⚠ Failed to load base font, fallback to Helvetica: {}", e.getMessage());
                    mainFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                }

                // 尝试加载 Bold（可选）
                try {
                    ClassPathResource boldRes = new ClassPathResource("fonts/NotoSansCJKsc-Bold.otf");
                    if (boldRes.exists()) {
                        try (InputStream boldIn = boldRes.getInputStream()) {
                            byte[] boldBytes = boldIn.readAllBytes();
                            FontProgram boldProgram = FontProgramFactory.createFont(boldBytes);
                            boldFont = PdfFontFactory.createFont(boldProgram, PdfEncodings.IDENTITY_H,
                                    PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
                            log.info("✓ Bold font loaded: {}", boldRes.getPath());
                        }
                    }
                } catch (Exception ex) {
                    log.warn("⚠ No bold font loaded (optional): {}", ex.getMessage());
                }

                // 如果包含 emoji，则加载 emoji 字体
                if (hasEmoji) {
                    try {
                        // 尝试从类路径加载emoji字体
                        InputStream emojiIn = getClass().getClassLoader().getResourceAsStream("fonts/NotoColorEmoji.ttf");
                        if (emojiIn != null) {
                            try {
                                byte[] emojiBytes = emojiIn.readAllBytes();
                                FontProgram emojiProgram = FontProgramFactory.createFont(emojiBytes);
                                emojiFont = PdfFontFactory.createFont(emojiProgram, PdfEncodings.IDENTITY_H,
                                        PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
                                log.info("✓ Emoji font loaded from classpath");
                            } finally {
                                emojiIn.close();
                            }
                        } else {
                            log.info("ℹ Emoji font not found in classpath (optional).");
                        }
                    } catch (Exception e) {
                        log.warn("⚠ Failed to load emoji font (optional): {}", e.getMessage());
                    }
                }

                // 3. Document 基本样式
                document.setFont(mainFont);
                document.setFontSize(11);
                document.setMargins(20, 30, 30, 30); // 顶部边距减少，更紧凑

                // 4. 解析Markdown并渲染（传入临时文件列表用于清理）
                parseMarkdownAndRender(document, cleanedContent, mainFont, boldFont, emojiFont, tempImageFiles);
            } // try writer/pdf/document

            // 上传到 COS（调用你原来的逻辑）
            String aiMessageId = UUID.randomUUID().toString();
            if (cosManager.uploadPDFFile(filePath, pdfFile)) {
                String pdfUrl = cosManager.getPDFUrl(filePath);
                asyncTaskService.executeAsyncTask(() -> {
                    UserFile userFile = UserFile.builder()
                            .fileUrl(pdfUrl)
                            .userId(Long.parseLong(safeUserId))
                            .fileName(fileName)
                            .createTime(new Date())
                            .updateTime(new Date())
                            .build();
                    ChatMessage aiMessage = ChatMessage.builder()
                            .id(aiMessageId)
                            .chatId(safeUserId)
                            .sessionId(safeSessionId)
                            .messageType(MessageType.TEXT)
                            .isAiResponse(true)
                            .metadata(MessageMetadata.builder()
                                    .pdfFileName(fileName)
                                    .pdfFileUrl(pdfUrl)
                                    .build())
                            .build();
                    fileService.save(userFile);
                    chatMessageService.save(aiMessage);
                }, "userId：" + safeUserId + " => 添加文件信息到数据库成功");
                return pdfFile.getName() + " generated successfully";
            } else {
                return "Error generating PDF";
            }

        } catch (IOException e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
        } finally {
            // 删除临时PDF文件
            cosManager.deleteTempFile(pdfFile);
            
            // 清理所有下载的临时图片文件
            cleanupTempImages(tempImageFiles);
        }
    }

    // ==================== Markdown解析与渲染 ====================

    /**
     * 解析Markdown内容并渲染到PDF
     * 
     * @param document PDF文档对象
     * @param content Markdown内容
     * @param regularFont 常规字体
     * @param boldFont 粗体字体
     * @param emojiFont Emoji字体
     * @param tempImageFiles 临时图片文件列表（用于后续清理）
     */
    private void parseMarkdownAndRender(Document document, String content,
            PdfFont regularFont, PdfFont boldFont, PdfFont emojiFont, List<Path> tempImageFiles) {
        String[] lines = content.split("\n");
        com.itextpdf.layout.element.List currentList = null; // 用于合并连续的列表项
        boolean inOrderedList = false; // 是否在有序列表中
        boolean lastWasEmpty = false; // 跟踪上一行是否为空
        boolean isFirstElement = true; // 标记是否为第一个元素

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // 空行：添加间距，并结束当前列表（避免连续空行）
            if (line.isEmpty()) {
                if (currentList != null) {
                    document.add(currentList);
                    currentList = null;
                }
                // 只在非连续空行时添加间距
                if (!lastWasEmpty && i > 0 && i < lines.length - 1) {
                    document.add(new Paragraph(" ").setMarginTop(3).setMarginBottom(0));
                }
                lastWasEmpty = true;
                continue;
            }

            lastWasEmpty = false; // 重置空行标记

            // 1️⃣ 一级标题 #
            if (line.startsWith("# ") && !line.startsWith("## ")) {
                if (currentList != null) {
                    document.add(currentList);
                    currentList = null;
                }
                addTitle(document, line.substring(2).trim(), 22, boldFont != null ? boldFont : regularFont,
                        new DeviceRgb(41, 98, 255), regularFont, emojiFont, isFirstElement); // 蓝色
                isFirstElement = false; // 第一个元素已添加
                continue;
            }

            // 2️⃣ 二级标题 ##
            if (line.startsWith("## ") && !line.startsWith("### ")) {
                if (currentList != null) {
                    document.add(currentList);
                    currentList = null;
                }
                addTitle(document, line.substring(3).trim(), 18, boldFont != null ? boldFont : regularFont,
                        new DeviceRgb(74, 74, 74), regularFont, emojiFont, isFirstElement); // 深灰色
                isFirstElement = false;
                continue;
            }

            // 3️⃣ 三级标题 ###
            if (line.startsWith("### ")) {
                if (currentList != null) {
                    document.add(currentList);
                    currentList = null;
                }
                addTitle(document, line.substring(4).trim(), 15, boldFont != null ? boldFont : regularFont,
                        new DeviceRgb(100, 100, 100), regularFont, emojiFont, isFirstElement); // 中灰色
                isFirstElement = false;
                continue;
            }

            // 4️⃣ 分隔线 --- 或 ***
            if (line.matches("^[-*]{3,}$")) {
                if (currentList != null) {
                    document.add(currentList);
                    currentList = null;
                }
                addDivider(document);
                isFirstElement = false;
                continue;
            }

            // 5️⃣ 引用块 >
            if (line.startsWith("> ")) {
                if (currentList != null) {
                    document.add(currentList);
                    currentList = null;
                }
                addQuote(document, line.substring(2).trim(), regularFont, emojiFont, isFirstElement);
                isFirstElement = false;
                continue;
            }

            // 6️⃣ 图片 ![描述](URL)
            if (line.startsWith("![") && line.contains("](") && line.endsWith(")")) {
                if (currentList != null) {
                    document.add(currentList);
                    currentList = null;
                }
                addImageFromMarkdown(document, line, tempImageFiles, isFirstElement);
                isFirstElement = false;
                continue;
            }

            // 7️⃣ 无序列表 - 或 *
            if (line.matches("^[-*]\\s+.*")) {
                if (currentList == null || inOrderedList) {
                    if (currentList != null) {
                        document.add(currentList);
                    }
                    currentList = new com.itextpdf.layout.element.List();
                    currentList.setSymbolIndent(12);
                    currentList.setListSymbol("•"); // 使用圆点符号
                    // 第一个列表项顶部减少边距
                    if (isFirstElement) {
                        currentList.setMarginTop(0);
                    }
                    inOrderedList = false;
                }
                String itemText = line.replaceFirst("^[-*]\\s+", "");
                addListItem(currentList, itemText, regularFont, boldFont, emojiFont);
                isFirstElement = false;
                continue;
            }

            // 8️⃣ 有序列表 1. 2. 3.
            if (line.matches("^\\d+\\.\\s+.*")) {
                if (currentList == null || !inOrderedList) {
                    if (currentList != null) {
                        document.add(currentList);
                    }
                    currentList = new com.itextpdf.layout.element.List();
                    currentList.setSymbolIndent(12);
                    // 第一个列表项顶部减少边距
                    if (isFirstElement) {
                        currentList.setMarginTop(0);
                    }
                    inOrderedList = true;
                }
                String itemText = line.replaceFirst("^\\d+\\.\\s+", "");
                addListItem(currentList, itemText, regularFont, boldFont, emojiFont);
                isFirstElement = false;
                continue;
            }

            // 9️⃣ 普通段落（处理内联格式：粗体、斜体、图片）
            if (currentList != null) {
                document.add(currentList);
                currentList = null;
            }
            addFormattedParagraph(document, line, regularFont, boldFont, emojiFont, tempImageFiles, isFirstElement);
            isFirstElement = false;
        }

        // 添加最后未完成的列表
        if (currentList != null) {
            document.add(currentList);
        }
    }

    // ==================== 图片处理方法 ====================

    /**
     * 从Markdown图片语法解析并添加图片到PDF
     * 支持格式：![描述](URL)
     * 
     * @param document PDF文档对象
     * @param line Markdown图片行
     * @param tempImageFiles 临时图片文件列表
     * @param isFirst 是否为第一个元素
     */
    private void addImageFromMarkdown(Document document, String line, List<Path> tempImageFiles, boolean isFirst) {
        // 使用正则表达式解析 ![描述](URL) 格式
        Pattern pattern = Pattern.compile("!\\[(.+?)\\]\\((.+?)\\)");
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            String description = matcher.group(1); // 图片描述
            String imageUrl = matcher.group(2);    // 图片URL

            log.info("解析到图片：描述={}, URL={}", description, imageUrl);

            // 下载并添加图片
            addImageToPdf(document, imageUrl, description, tempImageFiles, isFirst);
        }
    }

    /**
     * 下载图片并添加到PDF文档
     * 
     * @param document PDF文档对象
     * @param imageUrl 图片URL
     * @param description 图片描述（用于alt文本）
     * @param tempImageFiles 临时图片文件列表
     * @param isFirst 是否为第一个元素
     */
    private void addImageToPdf(Document document, String imageUrl, String description, 
            List<Path> tempImageFiles, boolean isFirst) {
        try {
            // 1. 下载图片到临时文件
            Path tempImagePath = downloadImage(imageUrl);
            if (tempImagePath == null) {
                log.warn("图片下载失败，跳过：{}", imageUrl);
                // 添加占位文本
                Paragraph placeholder = new Paragraph("[图片加载失败: " + description + "]")
                        .setFontColor(ColorConstants.GRAY)
                        .setFontSize(10)
                        .setMarginTop(isFirst ? 0 : 8)
                        .setMarginBottom(8);
                document.add(placeholder);
                return;
            }

            // 记录临时文件以便后续清理
            tempImageFiles.add(tempImagePath);

            // 2. 创建iText Image对象
            ImageData imageData = ImageDataFactory.create(tempImagePath.toFile().getAbsolutePath());
            Image pdfImage = new Image(imageData);

            // 3. 自动缩放图片以适应页面宽度
            float originalWidth = imageData.getWidth();
            float originalHeight = imageData.getHeight();
            float scaleRatio = 1.0f;

            // 如果图片超过最大宽度，按比例缩放
            if (originalWidth > MAX_IMAGE_WIDTH) {
                scaleRatio = MAX_IMAGE_WIDTH / originalWidth;
            }

            // 如果缩放后高度仍然超过最大高度，再次缩放
            if (originalHeight * scaleRatio > MAX_IMAGE_HEIGHT) {
                scaleRatio = MAX_IMAGE_HEIGHT / originalHeight;
            }

            // 应用缩放
            if (scaleRatio < 1.0f) {
                pdfImage.scale(originalWidth * scaleRatio, originalHeight * scaleRatio);
            }

            // 4. 设置图片居中显示
            pdfImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            pdfImage.setMarginTop(isFirst ? 0 : 10);
            pdfImage.setMarginBottom(10);

            // 5. 添加图片到文档
            document.add(pdfImage);

            // 6. 添加图片描述（如果有）
            if (description != null && !description.isEmpty()) {
                Paragraph caption = new Paragraph(description)
                        .setFontSize(9)
                        .setFontColor(ColorConstants.GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(8);
                document.add(caption);
            }

            log.info("图片添加成功：{}, 原始尺寸={}x{}, 缩放比例={}", 
                    description, originalWidth, originalHeight, scaleRatio);

        } catch (Exception e) {
            log.error("添加图片到PDF失败：{}", imageUrl, e);
            // 添加错误提示
            Paragraph errorPara = new Paragraph("[图片处理错误: " + description + "]")
                    .setFontColor(ColorConstants.RED)
                    .setFontSize(10)
                    .setMarginTop(isFirst ? 0 : 8)
                    .setMarginBottom(8);
            document.add(errorPara);
        }
    }

    /**
     * 从URL下载图片到临时文件
     * 
     * @param imageUrl 图片URL
     * @return 临时文件路径，下载失败返回null
     */
    private Path downloadImage(String imageUrl) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;

        try {
            // 1. 创建HTTP连接
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(DOWNLOAD_TIMEOUT);
            connection.setReadTimeout(DOWNLOAD_TIMEOUT);

            // 设置常见的请求头，模拟浏览器访问
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            connection.setRequestProperty("Accept", "image/*");

            // 2. 检查响应码
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.warn("图片下载失败，HTTP响应码：{}, URL：{}", responseCode, imageUrl);
                return null;
            }

            // 3. 获取输入流
            inputStream = connection.getInputStream();
            outputStream = new ByteArrayOutputStream();

            // 4. 读取图片数据
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            byte[] imageBytes = outputStream.toByteArray();
            if (imageBytes.length == 0) {
                log.warn("下载的图片数据为空：{}", imageUrl);
                return null;
            }

            // 5. 确定文件扩展名
            String extension = getImageExtension(imageUrl, connection.getContentType());

            // 6. 创建临时文件并写入数据
            Path tempFile = Files.createTempFile("pdf_image_", extension);
            Files.write(tempFile, imageBytes);

            log.info("图片下载成功：{}, 大小：{} bytes", imageUrl, imageBytes.length);
            return tempFile;

        } catch (Exception e) {
            log.error("下载图片异常：{}", imageUrl, e);
            return null;
        } finally {
            // 7. 关闭资源
            try {
                if (outputStream != null) outputStream.close();
                if (inputStream != null) inputStream.close();
                if (connection != null) connection.disconnect();
            } catch (IOException e) {
                log.warn("关闭资源时出错", e);
            }
        }
    }

    /**
     * 根据URL或Content-Type确定图片扩展名
     * 
     * @param url 图片URL
     * @param contentType HTTP响应的Content-Type
     * @return 文件扩展名（包含点号，如.jpg）
     */
    private String getImageExtension(String url, String contentType) {
        // 优先从URL中提取扩展名
        if (url != null) {
            String lowerUrl = url.toLowerCase();
            if (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg")) {
                return ".jpg";
            } else if (lowerUrl.contains(".png")) {
                return ".png";
            } else if (lowerUrl.contains(".gif")) {
                return ".gif";
            } else if (lowerUrl.contains(".webp")) {
                return ".webp";
            } else if (lowerUrl.contains(".bmp")) {
                return ".bmp";
            }
        }

        // 从Content-Type中确定扩展名
        if (contentType != null) {
            String lowerType = contentType.toLowerCase();
            if (lowerType.contains("jpeg") || lowerType.contains("jpg")) {
                return ".jpg";
            } else if (lowerType.contains("png")) {
                return ".png";
            } else if (lowerType.contains("gif")) {
                return ".gif";
            } else if (lowerType.contains("webp")) {
                return ".webp";
            } else if (lowerType.contains("bmp")) {
                return ".bmp";
            }
        }

        // 默认使用.jpg
        return ".jpg";
    }

    /**
     * 清理临时图片文件
     * 
     * @param tempImageFiles 临时图片文件列表
     */
    private void cleanupTempImages(List<Path> tempImageFiles) {
        if (tempImageFiles == null || tempImageFiles.isEmpty()) {
            return;
        }

        for (Path path : tempImageFiles) {
            try {
                if (Files.exists(path)) {
                    Files.delete(path);
                    log.debug("已清理临时图片文件：{}", path);
                }
            } catch (IOException e) {
                log.warn("清理临时图片文件失败：{}", path, e);
            }
        }
    }

    // ==================== 文本渲染方法 ====================

    /**
     * 添加标题
     */
    private void addTitle(Document document, String text, float fontSize, PdfFont font,
            DeviceRgb color, PdfFont regularFont, PdfFont emojiFont, boolean isFirst) {
        Paragraph title = new Paragraph();

        // 处理标题中的emoji
        if (containsEmoji(text) && emojiFont != null) {
            title.add(new Text(text).setFont(emojiFont).setFontSize(fontSize).setFontColor(color).setBold());
        } else {
            title.add(new Text(text).setFont(font).setFontSize(fontSize).setFontColor(color));
        }

        // 优化间距：第一个元素顶部不留空白，其他元素正常间距
        if (isFirst) {
            title.setMarginTop(0); // 第一个元素紧贴顶部
            title.setMarginBottom(8);
        } else if (fontSize >= 20) {
            title.setMarginTop(12);
            title.setMarginBottom(8);
        } else if (fontSize >= 16) {
            title.setMarginTop(10);
            title.setMarginBottom(6);
        } else {
            title.setMarginTop(8);
            title.setMarginBottom(5);
        }

        document.add(title);
    }

    /**
     * 添加分隔线
     */
    private void addDivider(Document document) {
        Paragraph divider = new Paragraph("─".repeat(50))
                .setFontSize(8)
                .setFontColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(8)
                .setMarginBottom(8);
        document.add(divider);
    }

    /**
     * 添加引用块
     */
    private void addQuote(Document document, String text, PdfFont regularFont, PdfFont emojiFont, boolean isFirst) {
        Paragraph quote = new Paragraph();

        // 使用简单的 > 符号或emoji表情
        if (containsEmoji(text) && emojiFont != null) {
            quote.add(new Text("💡 " + text).setFont(emojiFont));
        } else {
            // 使用常规字符避免渲染问题
            Text quoteText = new Text("► " + text).setFont(regularFont);
            quote.add(quoteText);
        }

        quote.setBackgroundColor(new DeviceRgb(245, 245, 245))
                .setPadding(10)
                .setMarginLeft(15)
                .setMarginTop(isFirst ? 0 : 6) // 第一个元素顶部无边距
                .setMarginBottom(6)
                .setBorderLeft(new com.itextpdf.layout.borders.SolidBorder(new DeviceRgb(100, 149, 237), 3));
        document.add(quote);
    }

    /**
     * 添加列表项
     */
    private void addListItem(com.itextpdf.layout.element.List list, String text, PdfFont regularFont, PdfFont boldFont, PdfFont emojiFont) {
        Paragraph itemPara = parseInlineFormatting(text, regularFont, boldFont, emojiFont);
        itemPara.setMarginTop(2).setMarginBottom(2);

        ListItem item = new ListItem();
        item.add(itemPara);
        list.add(item);
    }

    /**
     * 添加带格式的段落（支持内联图片）
     * 
     * @param document PDF文档对象
     * @param line 文本行
     * @param regularFont 常规字体
     * @param boldFont 粗体字体
     * @param emojiFont Emoji字体
     * @param tempImageFiles 临时图片文件列表
     * @param isFirst 是否为第一个元素
     */
    private void addFormattedParagraph(Document document, String line, PdfFont regularFont,
            PdfFont boldFont, PdfFont emojiFont, List<Path> tempImageFiles, boolean isFirst) {
        // 检查是否包含内联图片 ![描述](URL)
        Pattern imagePattern = Pattern.compile("!\\[(.+?)\\]\\((.+?)\\)");
        Matcher imageMatcher = imagePattern.matcher(line);

        if (imageMatcher.find()) {
            // 包含图片，需要分段处理
            int lastEnd = 0;
            while (imageMatcher.find()) {
                // 添加图片前的文本
                if (imageMatcher.start() > lastEnd) {
                    String textBefore = line.substring(lastEnd, imageMatcher.start()).trim();
                    if (!textBefore.isEmpty()) {
                        Paragraph textPara = parseInlineFormatting(textBefore, regularFont, boldFont, emojiFont);
                        textPara.setMarginTop(isFirst ? 0 : 4).setMarginBottom(4);
                        document.add(textPara);
                        isFirst = false;
                    }
                }

                // 添加图片
                String description = imageMatcher.group(1);
                String imageUrl = imageMatcher.group(2);
                addImageToPdf(document, imageUrl, description, tempImageFiles, isFirst);
                isFirst = false;

                lastEnd = imageMatcher.end();
            }

            // 添加图片后的文本
            if (lastEnd < line.length()) {
                String textAfter = line.substring(lastEnd).trim();
                if (!textAfter.isEmpty()) {
                    Paragraph textPara = parseInlineFormatting(textAfter, regularFont, boldFont, emojiFont);
                    textPara.setMarginTop(4).setMarginBottom(4);
                    document.add(textPara);
                }
            }
        } else {
            // 不包含图片，正常处理文本
            Paragraph para = parseInlineFormatting(line, regularFont, boldFont, emojiFont);
            para.setMarginTop(isFirst ? 0 : 4).setMarginBottom(4); // 第一个元素顶部无边距
            document.add(para);
        }
    }

    /**
     * 解析内联格式（粗体**）- 使用正则表达式精确匹配
     */
    private Paragraph parseInlineFormatting(String text, PdfFont regularFont, PdfFont boldFont, PdfFont emojiFont) {
        Paragraph para = new Paragraph();

        if (text == null || text.isEmpty()) {
            return para;
        }

        // 使用正则表达式匹配 **粗体文本**
        Pattern pattern = Pattern.compile("\\*\\*(.+?)\\*\\*");
        Matcher matcher = pattern.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            // 添加粗体之前的普通文本
            if (matcher.start() > lastEnd) {
                String normalText = text.substring(lastEnd, matcher.start());
                addTextWithEmoji(para, normalText, regularFont, emojiFont, false);
            }

            // 添加粗体文本（group(1) 是 ** 之间的内容）
            String boldText = matcher.group(1);
            PdfFont useFont = (boldFont != null) ? boldFont : regularFont;
            addTextWithEmoji(para, boldText, useFont, emojiFont, true);

            lastEnd = matcher.end();
        }

        // 添加剩余的普通文本
        if (lastEnd < text.length()) {
            String remainingText = text.substring(lastEnd);
            addTextWithEmoji(para, remainingText, regularFont, emojiFont, false);
        }

        return para;
    }

    /**
     * 添加文本（自动处理emoji）
     * 注意：传入的font参数已经根据isBold选择了正确的字体（boldFont或regularFont）
     */
    private void addTextWithEmoji(Paragraph para, String text, PdfFont font, PdfFont emojiFont, boolean isBold) {
        if (text == null || text.isEmpty()) {
            return;
        }

        if (containsEmoji(text) && emojiFont != null) {
            // 包含emoji时，需要分离emoji和普通文字，分别渲染
            StringBuilder normalText = new StringBuilder();
            for (int i = 0; i < text.length();) {
                int codePoint = text.codePointAt(i);
                if (codePoint > 0xFFFF) {
                    // 这是emoji或非BMP字符
                    // 先输出之前累积的普通文本
                    if (normalText.length() > 0) {
                        Text t = new Text(normalText.toString()).setFont(font);
                        para.add(t);
                        normalText.setLength(0);
                    }
                    // 输出emoji
                    String emojiChar = new String(Character.toChars(codePoint));
                    Text emojiText = new Text(emojiChar).setFont(emojiFont);
                    para.add(emojiText);
                    i += Character.charCount(codePoint);
                } else {
                    // 普通字符，累积
                    normalText.append((char) codePoint);
                    i++;
                }
            }
            // 输出剩余的普通文本
            if (normalText.length() > 0) {
                Text t = new Text(normalText.toString()).setFont(font);
                para.add(t);
            }
        } else {
            // 不包含emoji，直接使用传入的字体（已经是bold或regular）
            Text t = new Text(text).setFont(font);
            para.add(t);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 检测是否包含 emoji（或非 BMP 字符）
     */
    private boolean containsEmoji(String text) {
        if (text == null)
            return false;
        return text.codePoints().anyMatch(cp -> cp > 0xFFFF);
    }

    /**
     * 清洗字符串：去除零宽字符、控制符、非法代理项等（避免 iText 跳过渲染）
     */
    private String cleanContent(String input) {
        if (input == null)
            return "";
        String cleaned = input;

        // 移除控制字符（保留 \r \n \t）
        cleaned = cleaned.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

        // 移除零宽字符（ZWJ, ZWNJ, ZWSP 等）
        cleaned = cleaned.replaceAll("[\\u200B-\\u200D\\uFEFF]", "");

        // 移除孤立的 UTF-16 代理项（一般不会有，但保险）
        cleaned = cleaned.replaceAll("[\\uD800-\\uDFFF]", "");

        // 合并过多空行
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");

        // 去掉首尾空白
        cleaned = cleaned.trim();

        // 保证 UTF-8 编码安全
        cleaned = new String(cleaned.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        return cleaned;
    }
}
