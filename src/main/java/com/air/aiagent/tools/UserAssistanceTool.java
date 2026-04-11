package com.air.aiagent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 用户交互工具 - 当LLM遇到问题时向用户请求帮助
 *
 * 使用场景：
 * 1. 信息不足时询问用户
 * 2. 需要用户确认某些信息
 * 3. 遇到困难请求用户提供思路
 * 4. 需要用户补充必要参数或数据
 */
@Slf4j
@Component
public class UserAssistanceTool {


    /**
     * 寻求建议
     * 当LLM有多种解决方案时，询问用户偏好
     *
     * @param problemDescription 当前面临的问题
     * @param solutionA         方案A描述
     * @param solutionB         方案B描述
     * @param prosAndCons      可选的方案优缺点对比
     */
    @Tool(name = "askForAdvice", description = """
            Ask the user for advice when you have multiple solutions but unsure which to choose.

            Use this when:
            - You have multiple ways to solve a problem
            - You need user preference to decide
            - The user might have a preference you should respect
            """)
    public String askForAdvice(
            @ToolParam(description = "Description of the problem or task") String problemDescription,
            @ToolParam(description = "First solution option") String solutionA,
            @ToolParam(description = "Second solution option") String solutionB,
            @ToolParam(description = "Optional comparison of pros and cons", required = false) String prosAndCons) {

        StringBuilder response = new StringBuilder();
        response.append("💡 需要您的建议\n\n");
        response.append("任务：").append(problemDescription).append("\n\n");
        response.append("方案A：").append(solutionA).append("\n");
        response.append("方案B：").append(solutionB).append("\n");

        if (prosAndCons != null && !prosAndCons.isEmpty()) {
            response.append("\n对比分析：\n").append(prosAndCons).append("\n");
        }

        response.append("\n请告诉我您倾向哪个方案，或者有其他建议？");

        return response.toString();
    }
}
