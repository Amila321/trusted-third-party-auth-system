# Build stage
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files
COPY package.json package-lock.json ./

# Install dependencies
RUN npm ci

# Copy source
COPY . .

# Build for production
RUN npm run build

# Runtime stage
FROM node:20-alpine

WORKDIR /app

# Install lightweight http server for production
RUN npm install -g serve

# Copy built assets from builder
COPY --from=builder /app/dist ./dist

EXPOSE 5173

# Serve the app (or use Vite preview for development-like experience)
CMD ["serve", "-s", "dist", "-l", "5173"]

