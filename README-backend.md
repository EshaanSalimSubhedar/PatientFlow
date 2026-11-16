# PatientFlow Spring Boot backend

This backend uses Spring Boot 3.5, Camunda 8.9.16, PostgreSQL, and the eight Zeebe job types present in the supplied BPMN files.

## Local run

Requires Java 21+, Maven, PostgreSQL, and Camunda Run. From this folder:

```bash
mvn spring-boot:run
```

Default local configuration connects to PostgreSQL at `localhost:5432` and Camunda REST/gRPC at `localhost:8081`/`localhost:26500`.

## Docker run from the repository root

The root `docker-compose.yml` must set:

- `CAMUNDA_CLIENT_GRPC_ADDRESS=http://host.docker.internal:26500`
- `CAMUNDA_CLIENT_REST_ADDRESS=http://host.docker.internal:8081`
- `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/patientflow`

Then run `docker compose up --build` from the repository root.

## API

- `GET /api/health`
- `GET /api/referrals`
- `GET /api/referrals/{id}`
- `POST /api/referrals`

Example request:

```json
{
  "patientName": "Demo Patient",
  "age": 45,
  "reason": "Routine referral for assessment",
  "specialists": ["Cardiology", "Radiology", "Dietetics"],
  "diagnosticTypes": ["Blood Test", "Imaging", "ECG"],
  "consentGiven": true,
  "simulateVitalsUnavailable": false,
  "simulateCriticalResult": false
}
```

Set `simulateVitalsUnavailable` to `true` to exercise the `VITALS_UNAVAILABLE` boundary event. Set `simulateCriticalResult` to `true` to exercise the critical-result path. These are demonstration switches only.

All vitals, referrals, consultations, reports, emergency notifications, and EHR synchronization in this starter are synthetic simulations. Do not use it for real patient care or clinical decisions.
