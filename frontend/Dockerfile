FROM nginx:alpine
# Copy custom config
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Copy static files
COPY . /usr/share/nginx/html/
# Remove config files from the served directory
RUN rm -f /usr/share/nginx/html/nginx.conf /usr/share/nginx/html/Dockerfile
EXPOSE 80
