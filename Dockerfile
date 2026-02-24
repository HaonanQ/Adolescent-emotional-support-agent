# 构建阶段
FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/ai-agent-0.0.1-SNAPSHOT.jar app.jar

# 暴露端口
EXPOSE 8123

# 启动应用
CMD ["java", "-jar", "app.jar"]
