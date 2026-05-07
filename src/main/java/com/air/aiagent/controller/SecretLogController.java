package com.air.aiagent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@RestController
@Tag(name = "私密日志接口", description = "用于查看私密日志的接口，需谨慎使用")
public class SecretLogController {

    @Operation(summary = "查看主程序日志", description = "查看主程序最新1000行日志，自动3秒刷新")
    @GetMapping(value = "/admin/log/view/secret/qhn-main", produces = "text/html;charset=utf-8")
    public String viewLog() {
        try {
            // 获取jar所在目录
            String jarPath = new File("").getAbsolutePath();
            String logFile = jarPath + "/app.log"; // 你的日志文件名

            // 读取最后1000行
            List<String> allLines = Files.readAllLines(Paths.get(logFile));
            int start = Math.max(0, allLines.size() - 1000);
            List<String> lastLines = allLines.subList(start, allLines.size());

            // 拼接日志
            StringBuilder logContent = new StringBuilder();
            for (String line : lastLines) {
                logContent.append(escapeHtml(line)).append("\n");
            }

            // 高亮处理
            String highlighted = highlightLog(logContent.toString());

            // 返回HTML页面（兼容低版本JDK）
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>");
            html.append("<html>");
            html.append("<head>");
            html.append("<meta charset=\"utf-8\">");
            html.append("<title>主程序日志查看</title>");
            html.append("<meta http-equiv=\"refresh\" content=\"3\">");
            html.append("<style>");
            html.append("body { background: #1e1e1e; color: #dcdcdc; font-family: Consolas, monospace; font-size: 13px; line-height: 1.4; padding: 10px; }");
            html.append(".error { color: #ff4d4d; font-weight: bold; }");
            html.append(".warn { color: #ffcc00; }");
            html.append(".info { color: #9cdcfe; }");
            html.append(".debug { color: #4ec9b0; }");
            html.append("</style>");
            html.append("</head>");
            html.append("<body>");
            html.append("<h3>最新日志（最后1000行，自动3秒刷新）</h3>");
            html.append("<pre>").append(highlighted).append("</pre>");
            html.append("</body>");
            html.append("</html>");

            return html.toString();

        } catch (Exception e) {
            return "<h3 style='color:red'>读取日志失败：" + escapeHtml(e.getMessage()) + "</h3>";
        }
    }

    @Operation(summary = "查看图片搜索MCP服务日志", description = "查看图片搜索MCP服务最新1000行日志，自动3秒刷新")
    @GetMapping(value = "/admin/log/view/secret/qhn-mcpimage", produces = "text/html;charset=utf-8")
    public String viewmcpsearchLog() {
        try {
            // 获取jar所在目录
            String jarPath = new File("").getAbsolutePath();
            String logFile = jarPath + "/app2.log"; // 你的日志文件名

            // 读取最后1000行
            List<String> allLines = Files.readAllLines(Paths.get(logFile));
            int start = Math.max(0, allLines.size() - 1000);
            List<String> lastLines = allLines.subList(start, allLines.size());

            // 拼接日志
            StringBuilder logContent = new StringBuilder();
            for (String line : lastLines) {
                logContent.append(escapeHtml(line)).append("\n");
            }

            // 高亮处理
            String highlighted = highlightLog(logContent.toString());

            // 返回HTML页面（兼容低版本JDK）
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>");
            html.append("<html>");
            html.append("<head>");
            html.append("<meta charset=\"utf-8\">");
            html.append("<title>图片搜索MCP服务日志查看</title>");
            html.append("<meta http-equiv=\"refresh\" content=\"3\">");
            html.append("<style>");
            html.append("body { background: #1e1e1e; color: #dcdcdc; font-family: Consolas, monospace; font-size: 13px; line-height: 1.4; padding: 10px; }");
            html.append(".error { color: #ff4d4d; font-weight: bold; }");
            html.append(".warn { color: #ffcc00; }");
            html.append(".info { color: #9cdcfe; }");
            html.append(".debug { color: #4ec9b0; }");
            html.append("</style>");
            html.append("</head>");
            html.append("<body>");
            html.append("<h3>最新日志（最后1000行，自动3秒刷新）</h3>");
            html.append("<pre>").append(highlighted).append("</pre>");
            html.append("</body>");
            html.append("</html>");

            return html.toString();

        } catch (Exception e) {
            return "<h3 style='color:red'>读取日志失败：" + escapeHtml(e.getMessage()) + "</h3>";
        }
    }
    // 日志高亮
    private String highlightLog(String log) {
        return log
                .replaceAll("ERROR", "<span class='error'>ERROR</span>")
                .replaceAll("WARN", "<span class='warn'>WARN</span>")
                .replaceAll("INFO", "<span class='info'>INFO</span>")
                .replaceAll("DEBUG", "<span class='debug'>DEBUG</span>");
    }

    // HTML转义
    private String escapeHtml(String str) {
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
