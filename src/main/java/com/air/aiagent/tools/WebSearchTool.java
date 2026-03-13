package com.air.aiagent.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网页搜索工具
 */
public class WebSearchTool {

    // SearchAPI 的搜索接口地址
//    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";
    private static final String SEARCH_API_URL = "https://google.serper.dev/search"; // 新的

    private final String apiKey;

    public WebSearchTool(String apiKey) {
        this.apiKey = apiKey;
    }

//    @Tool(description = "Search for information from Baidu Search Engine")
//    public String searchWeb(
//            @ToolParam(description = "Search query keyword") String query) {
//        Map<String, Object> paramMap = new HashMap<>();
//        paramMap.put("q", query);
//        paramMap.put("api_key", apiKey);
//        paramMap.put("engine", "baidu");
//        try {
//            String response = HttpUtil.get(SEARCH_API_URL, paramMap);
//            // 取出返回结果的前 5 条
//            JSONObject jsonObject = JSONUtil.parseObj(response);
//            // 提取 organic_results 部分
//            JSONArray organicResults = jsonObject.getJSONArray("organic_results");
//            List<Object> objects = organicResults.subList(0, 5);
//            // 拼接搜索结果为字符串
//            String result = objects.stream().map(obj -> {
//                JSONObject tmpJSONObject = (JSONObject) obj;
//                return tmpJSONObject.toString();
//            }).collect(Collectors.joining(","));
//            return result;
//        } catch (Exception e) {
//            //如果失败返回一个调用失败的异常
//            return "Error searching Baidu: " + e.getMessage();
//        }
//    }

    /**
     * 使用 Serper API 进行 Google 搜索（POST 方式）
     *
     * @param query 搜索关键词
     * @return 搜索结果字符串
     */
    @Tool(description = "Search for information from Google Search Engine")
    public String searchWebBySerper(
            @ToolParam(description = "Search query keyword") String query) {
        try {
            // 构建请求体 JSON
            JSONObject requestBody = new JSONObject();
            requestBody.put("q", query);
            requestBody.put("gl", "cn");
            requestBody.put("hl", "zh-cn");

            // 发送 POST 请求
            HttpResponse response = HttpRequest.post(SEARCH_API_URL)
                    .header("X-API-KEY", apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .execute();

            // 检查响应状态
            if (!response.isOk()) {
                return "Error: HTTP " + response.getStatus() + " - " + response.body();
            }

            // 解析返回结果
            String responseBody = response.body();
            JSONObject jsonObject = JSONUtil.parseObj(responseBody);

            // 提取 organic 部分
            JSONArray organicResults = jsonObject.getJSONArray("organic");
            if (organicResults == null || organicResults.isEmpty()) {
                return "No results found";
            }

            // 取出返回结果的前 5 条
            int limit = Math.min(5, organicResults.size());
            List<Object> objects = organicResults.subList(0, limit);

            // 拼接搜索结果为字符串
            String result = objects.stream().map(obj -> {
                JSONObject tmpJSONObject = (JSONObject) obj;
                return tmpJSONObject.toString();
            }).collect(Collectors.joining(","));

            return result;
        } catch (Exception e) {
            return "Error searching Google via Serper: " + e.getMessage();
        }
    }
}
