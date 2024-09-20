FROM node:22.0-alpine

RUN mkdir -p /home/node/app/node_modules && chown -R node:node /home/node/
WORKDIR /home/node/app
COPY package*.json ./
USER node
RUN npm install
COPY --chown=node:node . .
CMD [ "node", "init.js" ]