FROM node:lts-hydrogen as build
WORKDIR ui
COPY ./ ./
RUN yarn
RUN yarn build

FROM nginx as webserver
WORKDIR /usr/share/nginx/html
COPY ./nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build ./ui/dist ./
