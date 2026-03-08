package com.air.aiagent.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 意图识别器
 * 用于识别用户对话是否需要使用知识库
 */
@Slf4j
@Component
public class IntentRecognizer {

    /**
     * 知识库相关关键词 - 青少年常见情感问题
     * 当用户消息包含这些关键词时，启用 RAG 知识库检索
     */
    private static final List<String> KNOWLEDGE_KEYWORDS = Arrays.asList(
            "抑郁", "焦虑", "压力", "紧张", "失眠", "情绪", "情感", "心理",
            "孤独", "寂寞", "自卑", "自信", "自尊", "自我",
            "学习", "考试", "成绩", "升学", "学业", "作业", "复习",
            "朋友", "友谊", "同学", "关系", "人际", "社交", "相处",
            "家庭", "父母", "亲子", "沟通", "理解",
            "恋爱", "喜欢", "爱情", "暗恋", "表白",
            "迷茫", "困惑", "烦恼", "困扰", "痛苦", "难过",
            "怎么办", "如何", "怎么", "怎样", "求助", "帮助",
            "建议", "意见", "指导", "方法", "技巧"
    );

    /**
     * 排除关键词 - 这些关键词出现时不启用 RAG
     */
    private static final List<String> EXCLUDE_KEYWORDS = Arrays.asList(
            "天气", "时间", "日期", "星期",
            "笑话", "故事", "游戏", "玩",
            "唱歌", "音乐", "跳舞",
            "吃饭", "睡觉",
            "今天", "明天", "昨天",
            "你好", "嗨", "哈喽",
            "在吗", "在不"
    );

    /**
     * 知识库主题正则匹配
     */
    private static final Pattern ACADEMIC_PATTERN = Pattern.compile(
            ".*(学习|考试|成绩|作业|复习|预习|上课|老师|学校|大学|高中|初中|小学).*",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern EMOTIONAL_PATTERN = Pattern.compile(
            ".*(心情|情绪|情感|难过|伤心|开心|快乐|愤怒|生气|害怕|恐惧|焦虑|抑郁|压力).*",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern RELATIONSHIP_PATTERN = Pattern.compile(
            ".*(朋友|同学|同事|家人|父母|爸爸|妈妈|兄弟姐妹|恋爱|对象|伴侣|关系).*",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 判断是否需要使用 RAG 知识库
     *
     * @param message 用户消息
     * @return true 表示需要使用知识库，false 表示不需要
     */
    public boolean needKnowledgeBase(String message) {
        if (message == null || message.trim().isEmpty()) {
            log.debug("消息为空，不启用知识库");
            return false;
        }

        String lowerMessage = message.toLowerCase().trim();

        // 先检查排除关键词
        for (String keyword : EXCLUDE_KEYWORDS) {
            if (lowerMessage.contains(keyword)) {
                log.info("检测到排除关键词 [{}]，不启用知识库", keyword);
                return false;
            }
        }

        // 检查知识库相关关键词
        for (String keyword : KNOWLEDGE_KEYWORDS) {
            if (lowerMessage.contains(keyword)) {
                log.info("检测到知识库相关关键词 [{}]，启用 RAG 知识库", keyword);
                return true;
            }
        }

        // 正则匹配知识库主题
        if (ACADEMIC_PATTERN.matcher(lowerMessage).matches()) {
            log.info("检测到学业相关主题，启用 RAG 知识库");
            return true;
        }
        if (EMOTIONAL_PATTERN.matcher(lowerMessage).matches()) {
            log.info("检测到情感相关主题，启用 RAG 知识库");
            return true;
        }
        if (RELATIONSHIP_PATTERN.matcher(lowerMessage).matches()) {
            log.info("检测到人际关系相关主题，启用 RAG 知识库");
            return true;
        }

        // 如果消息长度超过一定阈值，也可能需要知识库
        if (message.length() > 30) {
            log.info("消息长度超过 30 字符，可能需要专业知识，启用 RAG 知识库");
            return true;
        }

        log.debug("未检测到知识库相关关键词，不启用 RAG 知识库");
        return false;
    }
}
