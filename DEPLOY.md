# Guía de Despliegue para SIAF Report

Esta guía detalla los pasos para desplegar el servicio `siaf-report` en el servidor Linux (64.176.18.67).

## Prerrequisitos
- Acceso SSH al servidor `64.176.18.67`.
- Java 21 instalado en el servidor (el mismo que usa `siaf-api`).

## 1. Construcción del Proyecto (Local)
Ejecuta el siguiente comando en tu máquina local dentro de la carpeta `siaf-report`:

```bash
./mvnw clean package -DskipTests
```

Esto generará la carpeta `target/quarkus-app/`.

## 2. Preparación del Servidor
Conéctate al servidor y crea el directorio de destino:

```bash
ssh root@64.176.18.67 "mkdir -p /opt/siaf-report"
```

## 3. Transferencia de Archivos
Copia los archivos generados y el archivo de servicio al servidor:

```bash
# Copiar la aplicación
scp -r target/quarkus-app/* root@64.176.18.67:/opt/siaf-report/

# Copiar el archivo de servicio systemd
scp siaf-report.service root@64.176.18.67:/etc/systemd/system/
```

## 4. Configuración del Servicio
En el servidor, recarga systemd y habilita el servicio:

```bash
ssh root@64.176.18.67
# Dentro del servidor:

# 1. Recargar configuración de systemd
systemctl daemon-reload

# 2. Habilitar el servicio para que inicie con el sistema
systemctl enable siaf-report

# 3. Iniciar el servicio
systemctl start siaf-report

# 4. Verificar estado
systemctl status siaf-report
```

## 5. Verificación
El servicio debería estar corriendo en el puerto **9091**.
Puedes verificar los logs con:

```bash
journalctl -u siaf-report -f
```
