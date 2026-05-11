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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final float MAX_IMAGE_WIDTH = 400f;
    private static final float MAX_IMAGE_HEIGHT = 350f;
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
        List<Path> tempImageFiles = new ArrayList<>();

        try {
            try (PdfWriter writer = new PdfWriter(pdfFile.getAbsolutePath());
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {

                String cleanedContent = cleanContent(content);

                PdfFont mainFont = loadRegularFont();
                PdfFont boldFont = loadBoldFont();

                document.setFont(mainFont);
                document.setFontSize(11);
                document.setMargins(20, 30, 30, 30);

                parseMarkdownAndRender(document, cleanedContent, mainFont, boldFont, tempImageFiles);
            }

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
            cosManager.deleteTempFile(pdfFile);
            cleanupTempImages(tempImageFiles);
        }
    }

    // ==================== 字体加载 ====================
    private PdfFont loadRegularFont() {
        try {
            InputStream in = getClass().getResourceAsStream("/fonts/NotoSansCJKsc-Regular.otf");
            if (in != null) {
                byte[] bytes = in.readAllBytes();
                in.close();
                FontProgram fp = FontProgramFactory.createFont(bytes);
                log.info("✓ 中文字体加载成功");
                return PdfFontFactory.createFont(fp, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
            }
        } catch (Exception e) {
            log.warn("中文字体加载失败，使用默认");
        }
        try {
            return PdfFontFactory.createFont(StandardFonts.HELVETICA);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PdfFont loadBoldFont() {
        try {
            ClassPathResource res = new ClassPathResource("fonts/NotoSansCJKsc-Bold.otf");
            if (res.exists()) {
                try (InputStream in = res.getInputStream()) {
                    byte[] bytes = in.readAllBytes();
                    FontProgram fp = FontProgramFactory.createFont(bytes);
                    return PdfFontFactory.createFont(fp, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
                }
            }
        } catch (Exception e) {
            log.warn("粗体字体加载失败（可选）");
        }
        return null;
    }

    // ==================== Markdown解析 ====================
    private void parseMarkdownAndRender(Document document, String content,
                                        PdfFont regularFont, PdfFont boldFont, List<Path> tempImageFiles) {
        String[] lines = content.split("\n");
        com.itextpdf.layout.element.List currentList = null;
        boolean inOrderedList = false;
        boolean lastWasEmpty = false;
        boolean isFirstElement = true;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                if (currentList != null) {
                    document.add(currentList);
                    currentList = null;
                }
                if (!lastWasEmpty && i > 0 && i < lines.length - 1) {
                    document.add(new Paragraph(" ").setMarginTop(3).setMarginBottom(0));
                }
                lastWasEmpty = true;
                continue;
            }
            lastWasEmpty = false;

            if (line.startsWith("# ") && !line.startsWith("## ")) {
                if (currentList != null) { document.add(currentList); currentList = null; }
                addTitle(document, line.substring(2).trim(), 22, boldFont != null ? boldFont : regularFont,
                        new DeviceRgb(41, 98, 255), isFirstElement);
                isFirstElement = false; continue;
            }
            if (line.startsWith("## ") && !line.startsWith("### ")) {
                if (currentList != null) { document.add(currentList); currentList = null; }
                addTitle(document, line.substring(3).trim(), 18, boldFont != null ? boldFont : regularFont,
                        new DeviceRgb(74, 74, 74), isFirstElement);
                isFirstElement = false; continue;
            }
            if (line.startsWith("### ")) {
                if (currentList != null) { document.add(currentList); currentList = null; }
                addTitle(document, line.substring(4).trim(), 15, boldFont != null ? boldFont : regularFont,
                        new DeviceRgb(100, 100, 100), isFirstElement);
                isFirstElement = false; continue;
            }
            if (line.matches("^[-*]{3,}$")) {
                if (currentList != null) { document.add(currentList); currentList = null; }
                addDivider(document);
                isFirstElement = false; continue;
            }
            if (line.startsWith("> ")) {
                if (currentList != null) { document.add(currentList); currentList = null; }
                addQuote(document, line.substring(2).trim(), regularFont, isFirstElement);
                isFirstElement = false; continue;
            }
            if (line.startsWith("![") && line.contains("](") && line.endsWith(")")) {
                if (currentList != null) { document.add(currentList); currentList = null; }
                addImageFromMarkdown(document, line, tempImageFiles, isFirstElement);
                isFirstElement = false; continue;
            }
            if (line.matches("^[-*]\\s+.*")) {
                if (currentList == null || inOrderedList) {
                    if (currentList != null) document.add(currentList);
                    currentList = new com.itextpdf.layout.element.List();
                    currentList.setSymbolIndent(12);
                    currentList.setListSymbol("•");
                    if (isFirstElement) currentList.setMarginTop(0);
                    inOrderedList = false;
                }
                String itemText = line.replaceFirst("^[-*]\\s+", "");
                addListItem(currentList, itemText, regularFont, boldFont);
                isFirstElement = false; continue;
            }
            if (line.matches("^\\d+\\.\\s+.*")) {
                if (currentList == null || !inOrderedList) {
                    if (currentList != null) document.add(currentList);
                    currentList = new com.itextpdf.layout.element.List();
                    currentList.setSymbolIndent(12);
                    if (isFirstElement) currentList.setMarginTop(0);
                    inOrderedList = true;
                }
                String itemText = line.replaceFirst("^\\d+\\.\\s+", "");
                addListItem(currentList, itemText, regularFont, boldFont);
                isFirstElement = false; continue;
            }

            if (currentList != null) { document.add(currentList); currentList = null; }
            addFormattedParagraph(document, line, regularFont, boldFont, tempImageFiles, isFirstElement);
            isFirstElement = false;
        }
        if (currentList != null) document.add(currentList);
    }

    // ==================== 图片处理 ====================
    private void addImageFromMarkdown(Document document, String line, List<Path> tempImageFiles, boolean isFirst) {
        Pattern pattern = Pattern.compile("!\\[(.+?)\\]\\((.+?)\\)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String desc = matcher.group(1);
            String url = matcher.group(2);
            log.info("解析到图片：{}，路径：{}", desc, url);
            addImageToPdf(document, url, desc, tempImageFiles, isFirst);
        }
    }

    private void addImageToPdf(Document document, String imageUrl, String description,
                               List<Path> tempImageFiles, boolean isFirst) {
        try {
            Path tempImagePath = loadImageSmart(imageUrl);
            if (tempImagePath == null) {
                document.add(new Paragraph("[图片加载失败: " + description + "]")
                        .setFontColor(ColorConstants.GRAY).setFontSize(10)
                        .setMarginTop(isFirst ? 0 : 8).setMarginBottom(8));
                return;
            }
            tempImageFiles.add(tempImagePath);

            ImageData imageData = ImageDataFactory.create(Files.readAllBytes(tempImagePath));
            Image pdfImage = new Image(imageData);

            float w = imageData.getWidth();
            float h = imageData.getHeight();
            float scale = 1.0f;

            if (w > MAX_IMAGE_WIDTH) scale = MAX_IMAGE_WIDTH / w;
            if (h * scale > MAX_IMAGE_HEIGHT) scale = MAX_IMAGE_HEIGHT / h;

            pdfImage.scaleAbsolute(w * scale, h * scale);
            pdfImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            pdfImage.setMarginTop(10);
            pdfImage.setMarginBottom(10);

            document.add(pdfImage);

            if (description != null && !description.isBlank()) {
                document.add(new Paragraph(description)
                        .setFontSize(9)
                        .setFontColor(ColorConstants.GRAY)
                        .setTextAlignment(TextAlignment.CENTER));
            }

            log.info("✅ 图片添加成功：{}", description);

        } catch (Exception e) {
            log.error("添加图片失败：{}", imageUrl, e);
            document.add(new Paragraph("[图片处理错误]")
                    .setFontColor(ColorConstants.RED)
                    .setMarginTop(5)
                    .setMarginBottom(5));
        }
    }

    // 🔥 跨平台兼容图片加载
    private Path loadImageSmart(String imagePath) {
        try {
            if (imagePath.startsWith("http")) {
                URL url = new URL(imagePath);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setRequestProperty("Referer", "https://www.pexels.com/");
                conn.setRequestProperty("Accept", "image/*");

                if (conn.getResponseCode() != 200) {
                    log.warn("图片返回异常码：{}，URL：{}", conn.getResponseCode(), imagePath);
                    return null;
                }

                try (InputStream in = conn.getInputStream();
                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
                    byte[] data = out.toByteArray();

                    if (data.length < 1024) {
                        log.warn("下载内容不是图片：{}，大小={}字节", imagePath, data.length);
                        return null;
                    }

                    String ext = getImageExtension(imagePath, conn.getContentType());
                    Path tmp = Files.createTempFile("pdf_img_", ext);
                    Files.write(tmp, data);
                    log.info("✅ 网络图片下载成功：{}", imagePath);
                    return tmp;
                } finally {
                    conn.disconnect();
                }
            }

            String fileName = imagePath;
            if (fileName.startsWith("./")) {
                fileName = fileName.substring(2);
            }
            if (fileName.contains("/") || fileName.contains("\\")) {
                fileName = fileName.substring(Math.max(
                        fileName.lastIndexOf("/"),
                        fileName.lastIndexOf("\\")
                ) + 1);
            }

            Path tmpDir = Paths.get("tmp", "download").toAbsolutePath();
            File realFile = tmpDir.resolve(fileName).toFile();

            if (!realFile.exists()) {
                log.warn("本地文件不存在：{}", realFile.getAbsolutePath());
                return null;
            }

            byte[] data = Files.readAllBytes(realFile.toPath());
            String ext = getImageExtension(fileName, null);
            Path tmp = Files.createTempFile("pdf_img_", ext);
            Files.write(tmp, data);

            log.info("✅ 本地图片加载成功：{}", realFile.getAbsolutePath());
            return tmp;

        } catch (Exception e) {
            log.error("图片加载失败：{}", imagePath, e);
            return null;
        }
    }

    private String getImageExtension(String url, String contentType) {
        if (url != null) {
            String l = url.toLowerCase();
            if (l.contains(".jpg") || l.contains(".jpeg")) return ".jpg";
            if (l.contains(".png")) return ".png";
            if (l.contains(".gif")) return ".gif";
            if (l.contains(".webp")) return ".webp";
            if (l.contains(".bmp")) return ".bmp";
        }
        if (contentType != null) {
            String l = contentType.toLowerCase();
            if (l.contains("jpeg")) return ".jpg";
            if (l.contains("png")) return ".png";
            if (l.contains("gif")) return ".gif";
        }
        return ".jpg";
    }

    // ==================== 样式 ====================
    private Paragraph parseInlineFormatting(String text, PdfFont regular, PdfFont bold) {
        Paragraph p = new Paragraph();
        if (text == null || text.isBlank()) return p;

        Matcher m = Pattern.compile("\\*\\*(.+?)\\*\\*").matcher(text);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                p.add(new Text(text.substring(last, m.start())).setFont(regular));
            }
            p.add(new Text(m.group(1)).setFont(bold != null ? bold : regular));
            last = m.end();
        }
        if (last < text.length()) {
            p.add(new Text(text.substring(last)).setFont(regular));
        }
        return p;
    }

    private void addTitle(Document document, String text, float size, PdfFont font,
                          DeviceRgb color, boolean first) {
        Paragraph p = new Paragraph(text).setFont(font).setFontSize(size).setFontColor(color);
        if (first) p.setMarginTop(0).setMarginBottom(8);
        else if (size >= 20) p.setMarginTop(12).setMarginBottom(8);
        else p.setMarginTop(8).setMarginBottom(5);
        document.add(p);
    }

    private void addDivider(Document document) {
        document.add(new Paragraph("─".repeat(50))
                .setFontSize(8).setFontColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(8).setMarginBottom(8));
    }

    private void addQuote(Document document, String text, PdfFont font, boolean first) {
        Paragraph p = new Paragraph(text).setFont(font);
        p.setBackgroundColor(new DeviceRgb(245, 245, 245))
                .setPadding(10).setMarginLeft(15)
                .setMarginTop(first ? 0 : 6).setMarginBottom(6)
                .setBorderLeft(new com.itextpdf.layout.borders.SolidBorder(new DeviceRgb(100, 149, 237), 3));
        document.add(p);
    }

    private void addListItem(com.itextpdf.layout.element.List list, String text, PdfFont r, PdfFont b) {
        Paragraph para = parseInlineFormatting(text, r, b);
        para.setMarginTop(2).setMarginBottom(2);
        ListItem item = new ListItem();
        item.add(para);
        list.add(item);
    }

    private void addFormattedParagraph(Document document, String line, PdfFont r, PdfFont b, List<Path> imgs, boolean first) {
        Matcher m = Pattern.compile("!\\[(.+?)\\]\\((.+?)\\)").matcher(line);
        if (m.find()) {
            int last = 0;
            do {
                if (m.start() > last) {
                    String t = line.substring(last, m.start()).trim();
                    if (!t.isBlank()) {
                        Paragraph p = parseInlineFormatting(t, r, b);
                        p.setMarginTop(first ? 0 : 4).setMarginBottom(4);
                        document.add(p);
                        first = false;
                    }
                }
                addImageToPdf(document, m.group(2), m.group(1), imgs, first);
                first = false;
                last = m.end();
            } while (m.find());
            if (last < line.length()) {
                String t = line.substring(last).trim();
                if (!t.isBlank()) {
                    Paragraph p = parseInlineFormatting(t, r, b);
                    p.setMarginTop(4).setMarginBottom(4);
                    document.add(p);
                }
            }
        } else {
            Paragraph p = parseInlineFormatting(line, r, b);
            p.setMarginTop(first ? 0 : 4).setMarginBottom(4);
            document.add(p);
        }
    }

    private String cleanContent(String input) {
        if (input == null) return "";
        return input.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                .replaceAll("[\\u200B-\\u200D\\uFEFF]", "")
                .replaceAll("[\\uD800-\\uDFFF]", "")
                .replaceAll("\\n{3,}", "\n\n").trim();
    }

    private void cleanupTempImages(List<Path> tempImageFiles) {
        if (tempImageFiles == null || tempImageFiles.isEmpty()) return;
        for (Path path : tempImageFiles) {
            try {
                Files.deleteIfExists(path);
            } catch (Exception ignored) {}
        }
    }
}