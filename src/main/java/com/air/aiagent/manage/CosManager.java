package com.air.aiagent.manage;

import com.air.aiagent.config.CosClientConfig;
import com.air.aiagent.exception.BusinessException;
import com.air.aiagent.exception.ErrorCode;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.transfer.Download;
import com.qcloud.cos.transfer.TransferManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cos 对象存储操作
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Component
@Slf4j
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 下载 URL 前缀（从配置文件读取）
     */
    @Value("${cos.client.url}")
    private String downloadPrefix;

    /**
     * 上传对象
     *
     * @param key           唯一键
     * @param localFilePath 本地文件路径
     * @return
     */
    public PutObjectResult putObject(String key, String localFilePath) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                new File(localFilePath));
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     * @return
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }
    /**
     * 下载对象
     *
     * @param key 唯一键
     * @return
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }
    // 复用对象
    private TransferManager transferManager;

    // bean 加载完成后执行
    @PostConstruct
    public void init() {
        // 执行初始化逻辑
        System.out.println("Bean initialized!");
        // 多线程并发上传下载
        ExecutorService threadPool = Executors.newFixedThreadPool(32);
        transferManager = new TransferManager(cosClient, threadPool);
    }

    /**
     * 下载对象到本地文件
     *
     * @param key
     * @param localFilePath
     * @return
     * @throws InterruptedException
     */
    public Download download(String key, String localFilePath) throws InterruptedException {
        File downloadFile = new File(localFilePath);
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        Download download = transferManager.download(getObjectRequest, downloadFile);
        // 同步等待下载完成
        download.waitForCompletion();
        return download;
    }

    /**
     * 上传 MultipartFile 到 COS
     *
     * @param file       上传的文件
     * @param objectPath 存储路径
     * @return 文件访问 URL
     */
    public String uploadFile(MultipartFile file, String objectPath) {
        try {
            File tempFile = File.createTempFile("upload_", file.getOriginalFilename());
            file.transferTo(tempFile);
            
            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    cosClientConfig.getBucket(), 
                    objectPath,
                    tempFile);
            
            cosClient.putObject(putObjectRequest);
            
            log.info("文件上传成功：{} (大小：{} bytes)", objectPath, file.getSize());
            
            // 删除临时文件
            tempFile.delete();
            
            return getFileUrl(objectPath);
        } catch (Exception e) {
            log.error("文件上传失败：{}", objectPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传图片文件到 COS
     *
     * @param file       上传的 MultipartFile
     * @param objectPath 存储路径
     * @return 文件访问 URL
     */
    public String uploadImage(MultipartFile file, String objectPath) {
        try {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                contentType = "image/jpeg";
            }
            
            File tempFile = File.createTempFile("image_", file.getOriginalFilename());
            file.transferTo(tempFile);
            
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    cosClientConfig.getBucket(), 
                    objectPath,
                    tempFile);
            cosClient.putObject(putObjectRequest);
            
            log.info("图片上传成功：{} (大小：{} bytes)", objectPath, file.getSize());
            
            // 删除临时文件
            tempFile.delete();
            
            return getFileUrl(objectPath);
        } catch (Exception e) {
            log.error("图片上传失败：{}", objectPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        }
    }

    /**
     * 上传语音文件到 COS
     *
     * @param file       上传的 MultipartFile
     * @param objectPath 存储路径
     * @return 文件访问 URL
     */
    public String uploadAudio(MultipartFile file, String objectPath) {
        try {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("audio/")) {
                contentType = "audio/mpeg";
            }
            
            File tempFile = File.createTempFile("audio_", file.getOriginalFilename());
            file.transferTo(tempFile);
            
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    cosClientConfig.getBucket(), 
                    objectPath,
                    tempFile);
            
            cosClient.putObject(putObjectRequest);
            
            log.info("语音上传成功：{} (大小：{} bytes)", objectPath, file.getSize());
            
            // 删除临时文件
            tempFile.delete();
            
            return getFileUrl(objectPath);
        } catch (Exception e) {
            log.error("语音上传失败：{}", objectPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "语音上传失败");
        }
    }

    /**
     * 上传 PDF 文件到 COS
     *
     * @param objectPath 存储路径
     * @param pdfFile    PDF 文件对象
     * @return 是否上传成功
     */
    public boolean uploadPDFFile(String objectPath, File pdfFile) {
        try {
            if (pdfFile == null || pdfFile.length() == 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "PDF 文件无效");
            }
            
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    cosClientConfig.getBucket(), 
                    objectPath,
                    pdfFile);
            cosClient.putObject(putObjectRequest);
            
            log.info("PDF 文件上传成功：{} (大小：{} bytes)", objectPath, pdfFile.length());
            return true;
        } catch (Exception e) {
            log.error("PDF 文件上传失败：{} -> {}", pdfFile.getAbsolutePath(), objectPath, e);
            return false;
        }
    }

    /**
     * 获取文件 URL
     *
     * @param objectPath 文件存储路径
     * @return 文件 URL
     */
    public String getFileUrl(String objectPath) {
        return downloadPrefix + "/" + objectPath;
    }

    /**
     * 获取 PDF 文件 URL
     *
     * @param objectPath 文件存储路径
     * @return 文件 URL
     */
    public String getPDFUrl(String objectPath) {
        return getFileUrl(objectPath);
    }

    /**
     * 从 COS 读取图片并转换为 Base64 编码
     *
     * @param objectPath 图片路径
     * @return Base64 编码字符串（带 data:image/jpeg;base64,前缀）
     */
    public String getImageBase64(String objectPath) {
        if (objectPath == null || objectPath.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片路径不能为空");
        }

        try (COSObjectInputStream inputStream = cosClient.getObject(
                new GetObjectRequest(cosClientConfig.getBucket(), objectPath)).getObjectContent();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            byte[] imageBytes = outputStream.toByteArray();

            String base64Prefix = "data:image/jpeg;base64,";
            if (objectPath.endsWith(".png")) {
                base64Prefix = "data:image/png;base64,";
            }
            return base64Prefix + Base64.encodeBase64String(imageBytes);

        } catch (Exception e) {
            log.error("读取图片并转 Base64 失败：{}", objectPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片解析失败");
        }
    }

    /**
     * 从完整 URL 中提取 COS 对象路径
     *
     * @param fullImageUrl 完整 URL
     * @return 对象路径
     */
    public String extractObjectName(String fullImageUrl) {
        if (fullImageUrl == null || !fullImageUrl.startsWith(downloadPrefix)) {
            return fullImageUrl;
        }
        return fullImageUrl.substring(downloadPrefix.length() + 1);
    }

    /**
     * 创建临时图片目录
     *
     * @return 临时目录路径
     */
    private File createTmpImageDir() {
        String jarPath = System.getProperty("user.dir");
        File tmpDir = new File(jarPath, "tmpimage");

        if (!tmpDir.exists()) {
            boolean mkdirSuccess = tmpDir.mkdirs();
            if (mkdirSuccess) {
                log.info("创建临时图片目录成功：{}", tmpDir.getAbsolutePath());
            } else {
                log.error("创建临时图片目录失败：{}", tmpDir.getAbsolutePath());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建临时图片目录失败");
            }
        }
        return tmpDir;
    }

    /**
     * 从 COS 下载图片到本地临时目录
     *
     * @param objectPath 图片路径
     * @return 本地临时文件对象
     */
    public File downloadImageToTmp(String objectPath) {
        if (objectPath == null || objectPath.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片路径不能为空");
        }

        File tmpDir = createTmpImageDir();
        String fileName = objectPath.substring(objectPath.lastIndexOf("/") + 1);
        File tmpFile = new File(tmpDir, fileName);

        try (COSObjectInputStream inputStream = cosClient.getObject(
                new GetObjectRequest(cosClientConfig.getBucket(), objectPath)).getObjectContent()) {

            Files.copy(inputStream, tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("图片下载到临时目录成功：{}", tmpFile.getAbsolutePath());
            return tmpFile;

        } catch (Exception e) {
            log.error("下载图片到临时目录失败：{}", objectPath, e);
            deleteTempFile(tmpFile);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片下载失败");
        }
    }

    /**
     * 安全删除临时文件
     *
     * @param file 文件对象
     */
    public void deleteTempFile(File file) {
        if (file != null && file.exists()) {
            try {
                boolean deleted = file.delete();
                if (deleted) {
                    log.debug("临时文件已删除：{}", file.getAbsolutePath());
                } else {
                    log.warn("临时文件删除失败：{}", file.getAbsolutePath());
                }
            } catch (SecurityException e) {
                log.warn("没有权限删除文件：{}", file.getAbsolutePath());
            }
        }
    }

    /**
     * 关闭资源
     */
    @PreDestroy
    public void shutdown() {
        if (transferManager != null) {
            transferManager.shutdownNow();
        }
        if (cosClient != null) {
            cosClient.shutdown();
        }
    }


}
