package com.air.aiagent.manage;

import com.air.aiagent.exception.BusinessException;
import com.air.aiagent.exception.ErrorCode;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import io.minio.GetObjectArgs;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author WyH524
 * @since 2025/9/27 20:47
 */
@Component
@Slf4j
public class MinioManage {

    /**
     * 从配置文件中读取
     */
    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.end-point}")
    private String endPoint;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * Minio 客户端
     */
    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        minioClient = MinioClient.builder()
                .credentials(accessKey, secretKey)
                .endpoint(endPoint)
                .build();
        
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("创建存储桶成功: {}", bucketName);
            }
            
            String policy = "{\n" +
                    "    \"Version\": \"2012-10-17\",\n" +
                    "    \"Statement\": [\n" +
                    "        {\n" +
                    "            \"Effect\": \"Allow\",\n" +
                    "            \"Principal\": \"*\",\n" +
                    "            \"Action\": [\"s3:GetObject\"],\n" +
                    "            \"Resource\": [\"arn:aws:s3:::" + bucketName + "/*\"]\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}";
            
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(policy)
                    .build());
            log.info("设置存储桶公开访问策略成功: {}", bucketName);
        } catch (Exception e) {
            log.warn("设置存储桶策略失败: {}", e.getMessage());
        }
    }


    /**
     * 上传生成的 PDF 文件到 MinIO 中
     * @param objectPath MinIO存储路径（如 "public/pdf/document.pdf"）
     * @param pdfFile 生成的PDF文件对象
     * @return 是否上传成功
     * @throws IllegalArgumentException 如果文件无效
     */
    public boolean uploadPDFFile(String objectPath, File pdfFile) {
        // 1. 参数校验
        if (pdfFile == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "PDF文件对象不能为null");
        }
        if (pdfFile.length() == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "PDF文件内容为空");
        }
        try {
            // 2. 对于PDF文件，直接指定MIME类型为application/pdf
            String contentType = "application/pdf";

            // 3. 上传到MinIO
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .filename(pdfFile.getAbsolutePath())
                            .contentType(contentType)
                            .build()
            );

            log.info("文件上传成功: {} (大小: {} bytes)", objectPath, pdfFile.length());
            return true;
        } catch (Exception e) {
            log.error("文件上传失败: {} -> {}", pdfFile.getAbsolutePath(), objectPath, e);
            return false;
        }
    }

    /**
     * 获取 PDF文件 的URL
     * @param objectPath 文件存储路径
     * @return 文件URL
     */
    public String getPDFUrl(String objectPath){
        return endPoint + "/" + bucketName + "/" + objectPath;
    }

    /**
     * 上传图片文件到 MinIO 中
     * @param file 上传的 MultipartFile
     * @param objectPath MinIO存储路径
     * @return 上传后的文件URL
     */
    public String uploadImage(MultipartFile file, String objectPath) {
        try {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                contentType = "image/jpeg";
            }
            
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectPath)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(contentType)
                    .build()
            );
            
            log.info("图片上传成功: {} (大小: {} bytes)", objectPath, file.getSize());
            return endPoint + "/" + bucketName + "/" + objectPath;
        } catch (Exception e) {
            log.error("图片上传失败: {}", objectPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        }
    }

    /**
     * 上传语音文件到 MinIO 中
     * @param file 上传的 MultipartFile
     * @param objectPath MinIO存储路径
     * @return 上传后的文件URL
     */
    public String uploadAudio(MultipartFile file, String objectPath) {
        try {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("audio/")) {
                contentType = "audio/mpeg";
            }
            
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectPath)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(contentType)
                    .build()
            );
            
            log.info("语音上传成功: {} (大小: {} bytes)", objectPath, file.getSize());
            return endPoint + "/" + bucketName + "/" + objectPath;
        } catch (Exception e) {
            log.error("语音上传失败: {}", objectPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "语音上传失败");
        }
    }

    /**
     * 获取文件URL
     * @param objectPath 文件存储路径
     * @return 文件URL
     */
    public String getFileUrl(String objectPath) {
        return endPoint + "/" + bucketName + "/" + objectPath;
    }


    /**
     * 安全删除临时文件
     */
    public void deleteTempFile(File file) {
        if (file != null && file.exists()) {
            try {
                boolean deleted = file.delete();
                if (deleted) {
                    log.debug("临时文件已删除: {}", file.getAbsolutePath());
                } else {
                    log.warn("临时文件删除失败: {}", file.getAbsolutePath());
                }
            } catch (SecurityException e) {
                log.warn("没有权限删除文件: {}", file.getAbsolutePath());
            }
        }
    }


    /**
     * 获取存储桶中的文件列表
     * @param prefix 路径前缀（如 "public/"），可传null
     * @return 文件路径列表
     */
    public List<String> listFiles(String prefix) {
        List<String> files = new ArrayList<>();
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            System.out.println("存储桶是否存在: " + exists); // 必须输出true

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .build()
            );

            for (Result<Item> result : results) {
                files.add(result.get().objectName());
            }
        } catch (Exception e) {
            System.err.println("获取文件列表失败: " + e.getMessage());
        }
        return files;
    }
    /**
     * 新增：从MinIO读取图片并转换为Base64编码（解决本地图片外网无法访问问题）
     * @param objectPath MinIO中的图片路径（如 "public/images/xxx.jpg"）
     * @return Base64编码字符串（带data:image/jpeg;base64,前缀）
     */
    public String getImageBase64(String objectPath) {
        // 参数校验
        if (objectPath == null || objectPath.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片路径不能为空");
        }

        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectPath)
                        .build());
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // 读取图片字节
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            byte[] imageBytes = outputStream.toByteArray();

            // 转换为Base64（带MIME前缀，模型可直接识别）
            String base64Prefix = "data:image/jpeg;base64,";
            // 兼容png格式
            if (objectPath.endsWith(".png")) {
                base64Prefix = "data:image/png;base64,";
            }
            return base64Prefix + Base64.encodeBase64String(imageBytes);

        } catch (ErrorResponseException e) {
            log.error("图片不存在: {}", objectPath, e);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        } catch (Exception e) {
            log.error("读取图片并转Base64失败: {}", objectPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片解析失败");
        }
    }

    /**
     * 新增：从完整URL中提取MinIO对象路径（辅助方法）
     * 示例：http://localhost:9000/teenai-docs/public/images/xxx.jpg → public/images/xxx.jpg
     */
    public String extractMinioObjectName(String fullImageUrl) {
        if (fullImageUrl == null || !fullImageUrl.startsWith(endPoint)) {
            return fullImageUrl; // 本身就是对象路径，直接返回
        }
        // 剥离 endPoint + "/" + bucketName + "/" 前缀
        String prefix = endPoint + "/" + bucketName + "/";
        return fullImageUrl.substring(prefix.length());
    }
    /**
     * 创建临时目录（与jar包同级的tmpimage文件夹）
     * @return 临时目录路径
     */
    private File createTmpImageDir() {
        // 获取当前jar包运行目录 + tmpimage
        String jarPath = System.getProperty("user.dir");
        File tmpDir = new File(jarPath, "tmpimage");

        if (!tmpDir.exists()) {
            boolean mkdirSuccess = tmpDir.mkdirs();
            if (mkdirSuccess) {
                log.info("创建临时图片目录成功: {}", tmpDir.getAbsolutePath());
            } else {
                log.error("创建临时图片目录失败: {}", tmpDir.getAbsolutePath());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建临时图片目录失败");
            }
        }
        return tmpDir;
    }
    /**
     * 从MinIO下载图片到本地tmpimage临时目录
     * @param objectPath MinIO中的图片路径（如 "public/images/xxx.jpg"）
     * @return 本地临时文件对象
     */
    public File downloadImageToTmp(String objectPath) {
        // 1. 参数校验
        if (objectPath == null || objectPath.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片路径不能为空");
        }

        // 2. 创建临时目录
        File tmpDir = createTmpImageDir();

        // 3. 提取文件名，构建本地临时文件路径
        String fileName = objectPath.substring(objectPath.lastIndexOf("/") + 1);
        File tmpFile = new File(tmpDir, fileName);

        // 4. 从MinIO下载文件到本地
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectPath)
                        .build())) {

            // 写入本地文件
            Files.copy(inputStream, tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("图片下载到临时目录成功: {}", tmpFile.getAbsolutePath());
            return tmpFile;

        } catch (ErrorResponseException e) {
            log.error("图片不存在: {}", objectPath, e);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        } catch (Exception e) {
            log.error("下载图片到临时目录失败: {}", objectPath, e);
            // 清理无效临时文件
            deleteTempFile(tmpFile);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片下载失败");
        }
    }
    @PreDestroy
    public void shutdown() throws Exception {
        if (minioClient != null) {
            minioClient.close(); // 释放资源
        }
    }
}
