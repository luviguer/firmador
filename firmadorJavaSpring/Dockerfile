FROM openjdk:21-slim-buster as compiler
WORKDIR /home/maven/project
COPY --chown=maven:maven . /home/maven/project


RUN ./mvnw package

FROM openjdk:21-slim-buster
# Argumentos que le pasaremos a la JVM, por ejemplo una instancia del depurador
ARG JVM_ARGS="-Xms8192m"
# Instalamos curl y borramos los registros de apt para ahorrar espacio
# RUN apt update && apt install -y curl # rm -rf /var/cache/apt/archives /var/lib/apt/lists

# Marcamos el directorio de trabajo como /app dentro del conector
WORKDIR /app
# Copiamos el jar del conector en el directorio de trabajo
#RUN ./mvnw package && ls -la /home/maven/project/target
COPY --from=compiler /home/maven/project/target/firmador-0.0.2-SNAPSHOT.jar /app/firmador.jar
COPY --from=compiler /home/maven/project/keystore.p12 /app/

# Seteamos la variable de entorno JVM_ARGS con el valor del argumento creado antes
ENV JVM_ARGS=$JVM_ARGS
# Solo para pruebas, actualiza apt, instala diversos paquetes de depuración, crea el alias ll y borramos los registros apt
# RUN apt update; apt install -y vim tcpflow jq iproute2 tcpdump; echo "alias ll='ls -laiFh --color=always'">>~/.bashrc
# Punto de entrada de la imagen, comando que iniciará el funcionamiento de la «máquina»
ENTRYPOINT [ "sh", "-c", "exec java $JVM_ARGS -jar firmador.jar"]