# Configuración de Nginx para SIAF Report

Para exponer el servicio `siaf-report` (que corre en el puerto 9091) a través de Nginx en el servidor `64.176.18.67`, debes agregar un bloque `location` a tu archivo de configuración existente.

## 1. Editar la configuración en el servidor

Conéctate al servidor y edita el archivo de configuración de Nginx (usualmente en `/etc/nginx/sites-available/default` o `/etc/nginx/conf.d/siaf.conf`).

```bash
ssh root@64.176.18.67
nano /etc/nginx/sites-available/default
# O el archivo que estés usando para SIAF
```

## 2. Agregar el bloque location

Dentro del bloque `server { ... }` donde ya tienes configurado `siaf-web` (root) y `siaf-api` (/api), agrega lo siguiente:

```nginx
    # Proxy para SIAF Report
    location /report/ {
        # Redirige las peticiones que empiezan con /report/ al servicio en puerto 9091
        proxy_pass http://localhost:9091/report/;
        
        # Cabeceras estándar para proxy
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Aumentar timeouts si los reportes son grandes o tardan en generarse
        proxy_read_timeout 300s;
        proxy_connect_timeout 75s;
    }
```

## 3. Verificar y Reiniciar

1.  Verifica que la sintaxis sea correcta:
    ```bash
    nginx -t
    ```

2.  Si sale "syntax is ok", recarga Nginx:
    ```bash
    systemctl reload nginx
    ```

## Ejemplo de cómo debería verse el archivo completo (Resumido)

```nginx
server {
    listen 80;
    server_name tu-dominio.com o 64.176.18.67;

    # Frontend (SIAF Web)
    location / {
        root /var/www/siaf-web; # O donde tengas el frontend
        try_files $uri $uri/ /index.html;
    }

    # Backend API (SIAF API)
    location /api/ {
        proxy_pass http://localhost:9090/api/;
        # ... headers ...
    }

    # === NUEVO: Reportes ===
    location /report/ {
        proxy_pass http://localhost:9091/report/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```
