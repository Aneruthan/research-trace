# Multi-stage build for ResearchTrace Frontend (React 19 + TypeScript + Vite)
FROM node:22-alpine AS builder

WORKDIR /app

# Copy dependency files and install cleanly
COPY package.json package-lock.json ./
RUN npm ci

# Copy frontend source files
COPY . .

# Set VITE_API_BASE_URL to empty string for relative /api proxying through Nginx
ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# Build production distribution
RUN npm run build

# Runtime Stage
FROM nginx:alpine AS runtime

# Install curl for container health check
RUN apk add --no-cache curl

# Copy Nginx reverse proxy configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy production build assets from builder
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=5s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
