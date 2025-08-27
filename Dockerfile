# 仅运行期镜像，体积小
FROM eclipse-temurin:17-jre-alpine

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8"

WORKDIR /app
VOLUME ["/app/config", "/app/logs", "/app/data"]

# 把第 1 步产出的胖 JAR 复制进来（文件名按你的实际替换）
COPY target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["sh","-c","java ${JAVA_TOOL_OPTIONS} -jar /app/app.jar --spring.profiles.active=prod --spring.config.additional-location=file:/app/config/"]