# Stage 1: Build Stage
FROM node:16 AS build-stage

# Set working directory inside the container
WORKDIR /app

# Copy package.json and package-lock.json
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy the rest of the source code
COPY . .

# Build the frontend for production
ARG WS_API_URL
ARG LOGIN_URL
ENV WS_API_URL=${WS_API_URL}
ENV LOGIN_URL=${LOGIN_URL}
RUN npm run build:prod

# Stage 2: Production Stage
FROM nginx:stable-alpine

# Copy the built files from the build stage
COPY --from=build-stage /app/dist /usr/share/nginx/html

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Expose the default HTTP port
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
