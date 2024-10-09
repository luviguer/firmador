FROM node:22.0-alpine
# Preparamos los directorios locales
#RUN mkdir -p /home/node/app/node_modules && chown -R node:node /home/node/
USER node
WORKDIR /home/node/app
# Instalamos los requisitos de la aplicación
COPY --chown=node:node app .
RUN npm install
# Copiamos los ficheros de la aplicación
# Comando para lanzar la aplicación
CMD [ "node", "init.js" ]