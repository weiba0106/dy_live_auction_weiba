# 后端 Dockerfile
# 构建: docker build -t dy-auction-server .
# 运行: docker run -p 8080:8080 dy-auction-server

FROM maven:3.8-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY dy-common/pom.xml dy-common/
COPY dy-dao/pom.xml dy-dao/
COPY dy-service/pom.xml dy-service/
COPY dy-api/pom.xml dy-api/
# 先下载依赖（层缓存）
RUN mvn dependency:go-offline -pl dy-api -am -B -q || true
COPY . .
RUN mvn clean package -DskipTests -pl dy-api -am -B -q

FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/dy-api/target/dy-api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
