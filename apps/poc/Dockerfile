# =========================
# Etapa 1 - Build da aplicação
# =========================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copia arquivos do Maven
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Baixa dependências primeiro (melhora cache)
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# Copia código fonte
COPY src src

# Gera o jar
RUN ./mvnw clean package -DskipTests

# =========================
# Etapa 2 - Imagem final
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copia o jar gerado
COPY --from=build /app/target/*.jar app.jar

# Porta padrão Spring Boot
EXPOSE 8080

# Configurações da JVM
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Inicialização
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]