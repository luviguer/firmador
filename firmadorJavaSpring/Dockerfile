FROM openjdk:21-slim-buster as compiler
WORKDIR /home/maven
COPY --chown=maven:maven project .
RUN ./mvnw package

FROM openjdk:21-slim-buster
# Argumentos que le pasaremos a la JVM, por ejemplo una instancia del depurador
ARG JVM_ARGS="-Xms8192m"
# Marcamos el directorio de trabajo como /app dentro del conector
WORKDIR /app
# Copiamos el jar del conector y el almacén de claves en el directorio de trabajo
COPY --from=compiler /home/maven/target/firmador-0.0.2-SNAPSHOT.jar ./firmador.jar
# Seteamos la variable de entorno JVM_ARGS con el valor del argumento creado antes
ENV JVM_ARGS=$JVM_ARGS
# Punto de entrada de la imagen, comando que iniciará el funcionamiento de la «máquina»
ENTRYPOINT [ "sh", "-c", "exec java $JVM_ARGS -jar firmador.jar"]