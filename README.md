# Export Service

A microservice for handling bulk upload and download operations for the Metrics Dashboard application.

## Features

- Bulk Excel upload with Plans/Overrides/Items worksheets
- CSV export functionality
- 5-thread parallel processing for large datasets
- Comprehensive validation and error handling

## Configuration

The application requires a configuration file to be created manually for security reasons.

### Setup Instructions

1. Create the configuration file:
```bash
mkdir -p src/main/resources
cp src/main/resources/application-template.yml src/main/resources/application.yml
```

2. Update the database credentials in `src/main/resources/application.yml` to match your environment, or set environment variables:
```bash
export DB_USERNAME=your_db_username
export DB_PASSWORD=your_db_password
```

## Running the Service

```bash
mvn spring-boot:run
```

The service will start on port 8081.

## API Endpoints

- `POST /api/export-service/upload/bulk` - Bulk Excel upload
- `GET /api/export-service/download/csv` - CSV export
- `GET /api/export-service/upload/template` - Excel template download

## Database

This service connects to the same PostgreSQL database as the main metrics dashboard, maintaining shared access to the Plan, Override, and Item tables.

## Security

Database credentials are managed through environment variables or can be configured in the application.yml file. The configuration file with actual credentials is not included in the repository for security reasons.

## Phase 1 Microservices Migration

This service represents Phase 1 of the microservices migration, extracting bulk upload and download functionality from the main metrics dashboard while maintaining shared database connectivity.

### Extracted Components

- Bulk upload processing with Excel file parsing
- 5-thread parallel processing for large datasets
- CSV export functionality
- Complete validation and exception handling
- All related entity, repository, and service classes

### Integration

The frontend API service routes upload/download operations to this service (port 8081) while dashboard operations remain with the main service (port 8080).
