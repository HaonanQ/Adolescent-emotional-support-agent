# 青少年情感陪伴智能体


## 项目简介

**青少年情感陪伴智能体** 专为大学生打造的智能情感咨询平台。基于 SpringAI+Qwen-Max大模型，通过 RAG 知识库增强、ToolCalling 工具调用、MCP 服务集成和游戏化交互四大核心技术，采用 MongoDB 多会话管理、PgVector 向量检索、Redis 缓存、腾讯云对象存储等技术栈，提供专业的情感指导与心理支持。

## 主要功能

- **智能对话系统**  
  - 多轮对话记忆：基于Spring AI的对话记忆功能，提供连贯的咨询体验。
  - RAG知识库问答：集成情感知识库，提供专业的建议和情感指导。
  - 情绪识别：智能分析用户情绪状态，提供针对性的情感支持。

  
  
- **智能文档生成**  
  - PDF报告生成：基于对话内容自动生成个性化恋爱成长报告。
  - 结构化输出：使用Spring AI的结构化输出功能，生成标准化的建议文档。
  - 中文字体支持：完美支持中文PDF生成。

- **丰富工具集成**  
  - 文件操作工具：支持文件上传、下载和管理。
  - 网页搜索工具：实时获取最新信息。
  - PDF生成工具：自动生成恋爱指导文档。
  - MCP服务集成：支持高德地图等第三方服务。

## 技术栈
<img align="center" src="https://skillicons.dev/icons?i=java,spring,mysql,postgresql,redis&theme=light" />
本项目主要使用 Java 开发，结合现代技术栈，确保性能与扩展性。

**后端技术栈：**
- Spring Boot 3.4.4 - 主框架
- Spring AI - AI对话框架
- 阿里云百炼大模型 - 核心AI能力
- MyBatis Plus - 数据持久层
- PostgreSQL + PgVector - 向量数据库
- Redis - 缓存和会话管理

## 安装与使用

1. **环境要求**
   - Java 21+
   - MySQL 8.0+
   - PostgreSQL 12+ (支持pgvector扩展)
   - Redis 6.0+
   - Maven 3.6+

2. **克隆项目**：`git clone https://github.com/your-username/ai-agent.git`
3. **进入项目目录**：`cd ai-agent`
4. **配置数据库**：
   ```sql
   -- 创建MySQL数据库
   CREATE DATABASE loveai;
   -- 创建PostgreSQL数据库并安装pgvector扩展
   CREATE DATABASE mydatabase;
   \c mydatabase;
   CREATE EXTENSION vector;
   ```
5. **配置应用**：修改 `application.yml` 中的数据库连接信息
6. **启动应用**：`mvn spring-boot:run`

---

## 目前功能实现分配与进度：
**用户端：**
1. 用户登录注册（前后端）✅
2. 个人信息页，含个人信息修改。（前后端）
3. 心情日记，情绪记录（前后端）✅
4. 用户与ai的多模态交互——图像，文字，语音 （前后端）✅
5. ai生成的报告下载✅
6. 查看会话，历史消息（前后端）✅

**管理员端：**

1. 用户信息列表，可查看用户情绪记录（最新的）（前后端）
2. 知识库管理，在线更新（前后端）✅✅

**大模型：**

1. 多模态大模型的配置与调用，对话记忆上下文✅
2. 知识库的读取和加载，分片优化✅❌
3. 对话记录的保存✅
4. 工具集成和MCP服务调用✅
5. PDF报告生成✅

**整体优化：**

1. 全局异常处理✅
2. 全局aop的拦截，检查✅
3. 前端界面优化
4. PDF生成报告内容格式美化
