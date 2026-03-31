package com.air.aiagent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB 配置类
 * 解决 LocalDateTime 时区问题，确保存储到 MongoDB 的时间为本地时间（中国时间）
 * 
 * @author Qiuhaonan
 */
@Configuration
public class MongoConfig {

    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 配置 MongoDB 自定义转换器
     * 解决 LocalDateTime 存储到 MongoDB 时的时区问题
     */
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new LocalDateTimeToDateConverter());
        converters.add(new DateToLocalDateTimeConverter());
        return new MongoCustomConversions(converters);
    }

    /**
     * LocalDateTime 转 Date 转换器
     * 写入 MongoDB 时，将 LocalDateTime 转换为 Date
     * 关键：使用中国时区，确保存储的是本地时间而非 UTC 时间
     */
    private static class LocalDateTimeToDateConverter implements Converter<LocalDateTime, java.util.Date> {
        @Override
        public java.util.Date convert(LocalDateTime source) {
            return java.util.Date.from(source.atZone(CHINA_ZONE).toInstant());
        }
    }

    /**
     * Date 转 LocalDateTime 转换器
     * 从 MongoDB 读取时，将 Date 转换为 LocalDateTime
     * 关键：使用中国时区，确保读取的是本地时间
     */
    private static class DateToLocalDateTimeConverter implements Converter<java.util.Date, LocalDateTime> {
        @Override
        public LocalDateTime convert(java.util.Date source) {
            return LocalDateTime.ofInstant(source.toInstant(), CHINA_ZONE);
        }
    }
}
