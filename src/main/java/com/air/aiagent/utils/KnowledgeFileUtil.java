package com.air.aiagent.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 知识库文件工具类
 * 处理知识库文件夹和文档文件的创建、删除等操作
 * @author AI Agent
 * @since 2025/3/12
 */
@Slf4j
public class KnowledgeFileUtil {

    /**
     * 知识库根目录名
     */
    private static final String KNOWLEDGE_ROOT_DIR = "knowledge";

    /**
     * 获取知识库根目录路径
     * @return 根目录File对象
     */
    public static File getKnowledgeRootDir() {
        String currentDir = System.getProperty("user.dir");
        return new File(currentDir, KNOWLEDGE_ROOT_DIR);
    }

    /**
     * 获取指定知识库的目录路径
     * @param knowledgeBaseName 知识库名称
     * @return 知识库目录File对象
     */
    public static File getKnowledgeBaseDir(String knowledgeBaseName) {
        return new File(getKnowledgeRootDir(), sanitizeFileName(knowledgeBaseName));
    }

    /**
     * 创建知识库文件夹
     * @param knowledgeBaseName 知识库名称
     * @return 是否创建成功
     */
    public static boolean createKnowledgeBaseDir(String knowledgeBaseName) {
        try {
            File dir = getKnowledgeBaseDir(knowledgeBaseName);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (created) {
                    log.info("创建知识库文件夹成功: {}", dir.getAbsolutePath());
                }
                return created;
            }
            log.info("知识库文件夹已存在: {}", dir.getAbsolutePath());
            return true;
        } catch (Exception e) {
            log.error("创建知识库文件夹失败: {}", knowledgeBaseName, e);
            return false;
        }
    }

    /**
     * 删除知识库文件夹及其所有内容
     * @param knowledgeBaseName 知识库名称
     * @return 是否删除成功
     */
    public static boolean deleteKnowledgeBaseDir(String knowledgeBaseName) {
        try {
            File dir = getKnowledgeBaseDir(knowledgeBaseName);
            if (dir.exists()) {
                deleteDirectory(dir);
                log.info("删除知识库文件夹成功: {}", dir.getAbsolutePath());
                return true;
            }
            log.warn("知识库文件夹不存在，无需删除: {}", dir.getAbsolutePath());
            return true;
        } catch (Exception e) {
            log.error("删除知识库文件夹失败: {}", knowledgeBaseName, e);
            return false;
        }
    }

    /**
     * 保存上传的文件到知识库目录
     * @param knowledgeBaseName 知识库名称
     * @param file 上传的文件
     * @return 保存后的文件路径，失败返回null
     */
    public static String saveDocument(String knowledgeBaseName, MultipartFile file) {
        try {
            // 1. 确保知识库文件夹存在
            File knowledgeBaseDir = getKnowledgeBaseDir(knowledgeBaseName);
            if (!knowledgeBaseDir.exists()) {
                boolean created = knowledgeBaseDir.mkdirs();
                if (!created) {
                    log.error("创建知识库文件夹失败: {}", knowledgeBaseDir.getAbsolutePath());
                    return null;
                }
            }

            // 2. 处理文件名，避免重复和非法字符
            String originalFilename = file.getOriginalFilename();
            String safeFilename = sanitizeFileName(originalFilename);
            File targetFile = new File(knowledgeBaseDir, safeFilename);

            // 3. 如果文件已存在，添加序号
            int counter = 1;
            String baseName = getBaseName(safeFilename);
            String extension = getExtension(safeFilename);
            while (targetFile.exists()) {
                String newFilename = baseName + "_" + counter + (extension.isEmpty() ? "" : "." + extension);
                targetFile = new File(knowledgeBaseDir, newFilename);
                counter++;
            }

            // 4. 保存文件
            file.transferTo(targetFile);
            log.info("保存文档成功: {}", targetFile.getAbsolutePath());

            // 5. 返回相对路径
            return KNOWLEDGE_ROOT_DIR + "/" + sanitizeFileName(knowledgeBaseName) + "/" + targetFile.getName();

        } catch (IOException e) {
            log.error("保存文档失败: {}", file.getOriginalFilename(), e);
            return null;
        }
    }

    /**
     * 删除文档文件
     * @param filePath 文件路径（相对路径或绝对路径）
     * @return 是否删除成功
     */
    public static boolean deleteDocument(String filePath) {
        try {
            File file;
            if (filePath.startsWith(KNOWLEDGE_ROOT_DIR + "/")) {
                // 相对路径
                file = new File(System.getProperty("user.dir"), filePath);
            } else {
                // 绝对路径或已经是相对于user.dir的路径
                file = new File(filePath);
            }

            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    log.info("删除文档文件成功: {}", file.getAbsolutePath());
                } else {
                    log.warn("删除文档文件失败: {}", file.getAbsolutePath());
                }
                return deleted;
            }
            log.warn("文档文件不存在，无需删除: {}", filePath);
            return true;
        } catch (Exception e) {
            log.error("删除文档文件失败: {}", filePath, e);
            return false;
        }
    }

    /**
     * 检查文件是否存在
     * @param filePath 文件路径
     * @return 是否存在
     */
    public static boolean isDocumentExists(String filePath) {
        File file;
        if (filePath.startsWith(KNOWLEDGE_ROOT_DIR + "/")) {
            file = new File(System.getProperty("user.dir"), filePath);
        } else {
            file = new File(filePath);
        }
        return file.exists();
    }

    /**
     * 获取文件大小
     * @param filePath 文件路径
     * @return 文件大小（字节），失败返回-1
     */
    public static long getDocumentSize(String filePath) {
        try {
            File file;
            if (filePath.startsWith(KNOWLEDGE_ROOT_DIR + "/")) {
                file = new File(System.getProperty("user.dir"), filePath);
            } else {
                file = new File(filePath);
            }
            return file.exists() ? file.length() : -1;
        } catch (Exception e) {
            log.error("获取文件大小失败: {}", filePath, e);
            return -1;
        }
    }

    /**
     * 递归删除目录及其所有内容
     * @param dir 要删除的目录
     */
    private static void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    /**
     * 清理文件名，移除非法字符
     * @param filename 原始文件名
     * @return 安全的文件名
     */
    private static String sanitizeFileName(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "unnamed";
        }
        // 移除或替换非法字符
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    /**
     * 获取文件名（不含扩展名）
     * @param filename 文件名
     * @return 基础名称
     */
    private static String getBaseName(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(0, lastDot) : filename;
    }

    /**
     * 获取文件扩展名
     * @param filename 文件名
     * @return 扩展名（不含点）
     */
    private static String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 && lastDot < filename.length() - 1 ? filename.substring(lastDot + 1) : "";
    }
}
