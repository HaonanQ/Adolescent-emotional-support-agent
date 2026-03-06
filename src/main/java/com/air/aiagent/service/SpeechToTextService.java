package com.air.aiagent.service;

import com.alibaba.dashscope.audio.asr.transcription.Transcription;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionParam;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;

/**
 * 语音转文字服务
 * 使用通义千问语音识别API
 */
@Slf4j
@Service
public class SpeechToTextService {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    /**
     * 将音频文件转换为文字
     * @param audioFile 音频文件
     * @return 识别的文字内容
     */
    public String transcribeAudio(File audioFile) {
        try {
            log.info("开始语音识别，文件: {}", audioFile.getName());

//            Transcription transcription = new Transcription();
//            TranscriptionParam param = TranscriptionParam.builder()
//                    .apiKey(apiKey)
//                    .model("paraformer-v2")
//                    .file(audioFile.getAbsolutePath())
//                    .build();
//
//            transcription.call(param);
//
//            StringBuilder result = new StringBuilder();
//            transcription.getResults().forEach(segment -> {
//                result.append(segment.getText());
//            });
//
//            String text = result.toString();
//            log.info("语音识别完成，结果: {}", text);
            String text ="hello";
            return text;
        } catch (ApiException e) {
            log.error("语音识别失败", e);
            throw new RuntimeException("语音识别失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将音频字节数组转换为文字
     * @param audioBytes 音频字节数组
     * @param fileExtension 文件扩展名（如 .wav, .mp3）
     * @return 识别的文字内容
     */
    public String transcribeAudio(byte[] audioBytes, String fileExtension) {
        try {
            File tempFile = File.createTempFile("audio_", fileExtension);
            Files.write(tempFile.toPath(), audioBytes);

            String result = transcribeAudio(tempFile);

            tempFile.delete();
            return result;
        } catch (Exception e) {
            log.error("语音识别失败", e);
            throw new RuntimeException("语音识别失败: " + e.getMessage(), e);
        }
    }
}
