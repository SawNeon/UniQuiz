FROM node:22-alpine AS construcao
WORKDIR /codigo
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:stable-alpine
COPY --from=construcao /codigo/dist /usr/share/nginx/html
EXPOSE 80 443
