# SIAF Report Service

Microservicio para la generación de reportes utilizando Quarkus y JasperReports.

## Requisitos

- Java 21
- Maven
- PostgreSQL

## Configuración

La configuración de la base de datos se encuentra en `src/main/resources/application.properties`.

## Ejecución

Para ejecutar en modo desarrollo:

```bash
./mvnw quarkus:dev
```

## Endpoints

- `GET /report`: Endpoint de prueba.
- `GET /report/test-db`: Prueba de conexión a base de datos.
