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
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.List;
import com.itextpdf.layout.element.ListItem;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * PDF生成工具 - 支持Markdown格式解析
 * 
 * 功能特性：
 * - 自动解析Markdown格式内容
 * - 支持多级标题 (#, ##, ###)
 * - 支持有序列表和无序列表
 * - 支持粗体(**text**)和斜体(*text*)
 * - 支持引用块(> text)
 * - 支持分隔线(---)
 * - 自动处理表情符号
 * - 云端存储集成(MinIO)
 * 
 * @author AI Assistant
 */
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
            - Emojis: 😊 💕 🎉

            Example:
            # Title
            ## Section
            - Point 1
            - Point 2
            **Important** content here!
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
                            System.out.println("✓ Base font loaded from classpath");
                        } finally {
                            baseIn.close();
                        }
                    } else {
                        // 回退到内置字体（不会支持中文/emoji）
                        System.err.println("⚠ Base font not found in classpath, fallback to Helvetica");
                        mainFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                    }
                } catch (Exception e) {
                    // 回退到内置字体（不会支持中文/emoji）
                    System.err.println("⚠ Failed to load base font, fallback to Helvetica: " + e.getMessage());
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
                            System.out.println("✓ Bold font loaded: " + boldRes.getPath());
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("⚠ No bold font loaded (optional): " + ex.getMessage());
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
                                System.out.println("✓ Emoji font loaded from classpath");
                            } finally {
                                emojiIn.close();
                            }
                        } else {
                            System.out.println("ℹ Emoji font not found in classpath (optional).");
                        }
                    } catch (Exception e) {
                        System.err.println("⚠ Failed to load emoji font (optional): " + e.getMessage());
                    }
                }

                // 3. Document 基本样式
                document.setFont(mainFont);
                document.setFontSize(11);
                document.setMargins(20, 30, 30, 30); // 顶部边距减少，更紧凑

                // 4. 解析Markdown并渲染
                parseMarkdownAndRender(document, cleanedContent, mainFont, boldFont, emojiFont);
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
            // 删除临时文件（由你的 cosManager 实现）
            cosManager.deleteTempFile(pdfFile);
        }
    }

    // ==================== Markdown解析与渲染 ====================

    /**
     * 解析Markdown内容并渲染到PDF
     */
    private void parseMarkdownAndRender(Document document, String content,
            PdfFont regularFont, PdfFont boldFont, PdfFont emojiFont) {
        String[] lines = content.split("\n");
        List currentList = null; // 用于合并连续的列表项
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

            // 6️⃣ 无序列表 - 或 *
            if (line.matches("^[-*]\\s+.*")) {
                if (currentList == null || inOrderedList) {
                    if (currentList != null) {
                        document.add(currentList);
                    }
                    currentList = new List();
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

            // 7️⃣ 有序列表 1. 2. 3.
            if (line.matches("^\\d+\\.\\s+.*")) {
                if (currentList == null || !inOrderedList) {
                    if (currentList != null) {
                        document.add(currentList);
                    }
                    currentList = new List();
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

            // 8️⃣ 普通段落（处理内联格式：粗体、斜体）
            if (currentList != null) {
                document.add(currentList);
                currentList = null;
            }
            addFormattedParagraph(document, line, regularFont, boldFont, emojiFont, isFirstElement);
            isFirstElement = false;
        }

        // 添加最后未完成的列表
        if (currentList != null) {
            document.add(currentList);
        }
    }

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
    private void addListItem(List list, String text, PdfFont regularFont, PdfFont boldFont, PdfFont emojiFont) {
        Paragraph itemPara = parseInlineFormatting(text, regularFont, boldFont, emojiFont);
        itemPara.setMarginTop(2).setMarginBottom(2);

        ListItem item = new ListItem();
        item.add(itemPara);
        list.add(item);
    }

    /**
     * 添加带格式的段落
     */
    private void addFormattedParagraph(Document document, String line, PdfFont regularFont,
            PdfFont boldFont, PdfFont emojiFont, boolean isFirst) {
        Paragraph para = parseInlineFormatting(line, regularFont, boldFont, emojiFont);
        para.setMarginTop(isFirst ? 0 : 4).setMarginBottom(4); // 第一个元素顶部无边距
        document.add(para);
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
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\*\\*(.+?)\\*\\*");
        java.util.regex.Matcher matcher = pattern.matcher(text);

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