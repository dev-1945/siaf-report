#!/bin/bash
set -e

NGINX_CONF="/etc/nginx/sites-available/default"

# Verificar si existe el archivo
if [ ! -f "$NGINX_CONF" ]; then
    echo "Error: No se encontró $NGINX_CONF. Por favor edita manualmente."
    exit 1
fi

# Verificar si ya está configurado
if grep -q "location /report/" "$NGINX_CONF"; then
    echo "Nginx ya está configurado para /report/. No se requieren cambios."
    exit 0
fi

echo "1. Creando respaldo de la configuración..."
cp "$NGINX_CONF" "$NGINX_CONF.bak-$(date +%s)"

echo "2. Inyectando configuración para /report/..."

# Definir el bloque de configuración
BLOCK='
    # Proxy para SIAF Report
    location /report/ {
        proxy_pass http://localhost:9091/report/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
    }
'

# Usar sed para insertar el bloque antes del último "}" del archivo
# Asume que el archivo termina cerrando el bloque server { ... }
sed -i '$s/}/'"$BLOCK"'\n}/' "$NGINX_CONF"

echo "3. Verificando sintaxis de Nginx..."
if nginx -t; then
    echo "4. Recargando Nginx..."
    systemctl reload nginx
    echo "¡Éxito! SIAF Report configurado correctamente."
else
    echo "Error en la sintaxis. Restaurando respaldo..."
    cp "$NGINX_CONF.bak-$(date +%s)" "$NGINX_CONF"
    exit 1
fi
