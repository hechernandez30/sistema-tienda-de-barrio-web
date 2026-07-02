# ============================================================
# Frontend Angular - build con Node y servido con Nginx
# ============================================================

# --- Etapa 1: compilar Angular ---
FROM node:20-alpine AS build
WORKDIR /app

# Instalamos dependencias con caché (solo se reinstala si cambian los package*.json).
COPY package*.json ./
RUN npm ci

# Copiamos el código y generamos la build de producción (usa environment.prod.ts).
COPY . .
RUN npm run build -- --configuration production

# --- Etapa 2: servir con Nginx ---
FROM nginx:alpine

# Configuración de Nginx (SPA + proxy /api hacia el backend).
COPY nginx.conf /etc/nginx/conf.d/default.conf

# El builder "application" de Angular deja los estáticos en dist/frontend/browser.
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
