# Guía de Despliegue Automático (GitHub Actions) - SIAF Report

Este repositorio está configurado para desplegarse automáticamente en el servidor `64.176.18.67` cada vez que se hace un push a la rama `main`.

## 1. Configuración de Secretos en GitHub

Para que el despliegue funcione, debes configurar los siguientes **Secrets** en el repositorio de GitHub (`Settings` -> `Secrets and variables` -> `Actions`):

| Nombre Secreto | Valor / Descripción |
| :--- | :--- |
| `SERVER_HOST` | `64.176.18.67` |
| `SERVER_USER` | `root` (o el usuario con permisos de despliegue) |
| `SSH_KEY` | La clave privada SSH (contenido de tu archivo `.pem` o `id_rsa`) que tiene acceso al servidor. |
| `KNOWN_HOSTS` | La huella digital del servidor. Puedes obtenerla ejecutando `ssh-keyscan -H 64.176.18.67` en tu terminal local. |

> **Nota:** Si ya tienes estos secretos configurados a nivel de Organización o Environment, no necesitas repetirlos aquí.

## 2. Preparación del Servidor (Solo una vez)

Antes del primer despliegue automático, asegúrate de que el servidor tenga la estructura base y el servicio configurado para usar la carpeta `current` (enlace simbólico).

### A. Crear directorios
Conéctate al servidor y ejecuta:
```bash
mkdir -p /opt/siaf-report
```

### B. Configurar el Servicio Systemd
El archivo `siaf-report.service` en este repositorio ya ha sido actualizado para apuntar a `/opt/siaf-report`.

1. Copia el archivo de servicio actualizado al servidor:
   ```bash
   # Desde tu máquina local en la carpeta del proyecto
   scp siaf-report.service root@64.176.18.67:/etc/systemd/system/
   ```

2. Recarga y reinicia en el servidor:
   ```bash
   ssh root@64.176.18.67 "systemctl daemon-reload && systemctl enable siaf-report"
   ```

## 3. Flujo de Despliegue

El archivo `.github/workflows/deploy-report.yml` realiza lo siguiente:
1. **Build:** Compila el proyecto Java/Quarkus usando Maven.
2. **Package:** Empaqueta la aplicación (`quarkus-app`).
3. **Upload:** Sube el paquete al servidor.
4. **Deploy:**
   - Detiene el servicio `siaf-report`.
   - Descomprime los archivos directamente en `/opt/siaf-report`.
   - Reinicia el servicio `siaf-report`.

## 4. Verificación

Después de un push a `main`, ve a la pestaña **Actions** en GitHub para ver el progreso.
Si es exitoso, verifica en el servidor:

```bash
systemctl status siaf-report
ls -l /opt/siaf-report/current
```
