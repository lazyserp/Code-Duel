# Stage 1: Build the React application
FROM node:20-alpine AS build
WORKDIR /app

# Copy dependency configs
COPY package*.json ./
RUN npm ci

# Copy source and config files
COPY . .

# Inject production API/WebSocket URLs if needed, or rely on window.location / relative URLs.
# In Vite, env vars are prefixed with VITE_. If needed, they are passed as build-args.
RUN npm run build

# Stage 2: Serve using Nginx
FROM nginx:1.25-alpine
COPY --from=build /app/dist /usr/share/nginx/html

# Copy the custom Nginx config for routing and proxying
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
