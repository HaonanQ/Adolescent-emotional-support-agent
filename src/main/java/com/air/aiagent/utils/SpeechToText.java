package com.air.aiagent.utils;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

/**
 * 语音转文字服务
 * 使用通义千问语音识别API
 */
@Slf4j
@Component
public class SpeechToText {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;
    @Value("${ai.audio.model}")
    private String audioModel;

    /**
     * 将音频文件转换为文字，并分析情感和内容（中文音频）
     * @param audioFile 音频文件（MP3格式）
     * @return 识别的文字内容 + 情感分析结果
     */
    public String transcribeAudio(File audioFile) {
        // 1. 校验文件是否存在
        if (!audioFile.exists()) {
            log.error("音频文件不存在，路径：{}", audioFile.getAbsolutePath());
            throw new RuntimeException("音频文件不存在");
        }

        try {
            log.info("开始语音识别，文件: {}，模型：{}", audioFile.getName(), audioModel);

            // 2. 构建文件绝对路径（格式：file:///xxx/xxx.mp3）
            final String localFilePath = "file://" + audioFile.getAbsolutePath().replace("\\", "/");
            //测试音频
            final String localFilePath2 ="file:///Users/beiyu/Java/adolescent-emotional-support-agent/LoveAI/tmpaudio/装修噪音.wav";
            // 兼容Windows系统路径（替换\为/，确保路径格式正确）
            log.info("音频文件路径：{}", localFilePath);

            // 3. 构建多模态消息（音频+文本指令）
            MultiModalMessage userMessage = MultiModalMessage.builder()
                    .role(Role.USER.getValue())
                    .content(Arrays.asList(
                            // 音频文件参数
                            new HashMap<String, Object>() {{
                                put("audio", localFilePath);
                            }},
                            // 文本指令：分析中文音频的情感和内容
                            new HashMap<String, Object>() {{
                                put("text", "分析音频所表达的情感和内容，音频为中文，请用中文描述");
                            }}
                    ))
                    .build();

            // 4. 构建API调用参数
            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .apiKey(apiKey) // 注入配置的API Key
                    .model(audioModel) // 使用配置的语音模型
                    .message(userMessage)
                    .build();

            // 5. 调用阿里云语音识别API
            MultiModalConversation conv = new MultiModalConversation();
            MultiModalConversationResult result = conv.call(param);

            // 6. 解析返回结果（提取文本内容）
            Object contentObj = result.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text");
            String textResult = contentObj != null ? contentObj.toString().trim() : "未识别到有效内容";

            log.info("语音识别完成，结果：{}", textResult);
            return textResult;

        } catch (ApiException e) {
            log.error("语音识别API调用失败", e);
            throw new RuntimeException("语音识别失败: " + e.getMessage(), e);
        } catch (NoApiKeyException e) {
            log.error("API Key未配置", e);
            throw new RuntimeException("语音识别失败: API Key未配置", e);
        } catch (UploadFileException e) {
            log.error("音频文件上传失败", e);
            throw new RuntimeException("语音识别失败: 音频文件处理异常", e);
        } catch (Exception e) {
            log.error("语音识别未知异常", e);
            throw new RuntimeException("语音识别失败: " + e.getMessage(), e);
        }
    }
}
