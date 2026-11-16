# PatientFlow — Multidisciplinary Patient Referral & Triage

PatientFlow is an academic workflow-orchestration prototype for a multidisciplinary patient referral process. It demonstrates how a referral can be created, assessed using synthetic observations, routed through BPMN process paths, coordinated across specialist and diagnostic activities, and passed to a reusable consent/EHR synchronization subprocess.

> **Safety notice:** PatientFlow is a university demonstration only. It uses synthetic/mock data and simulated actions. Its thresholds and workflow decisions are not clinical guidance. It must not be used for diagnosis, triage, treatment, or real patient care. No real EHR or clinical service is contacted.

## 1. Technology stack

| Layer | Technology |
|---|---|
| Frontend | React, Vite, Lucide React |
| Backend/API | Java 21, Spring Boot 3.5.5 |
| Workflow engine | Camunda 8.9.16 / Zeebe |
| Persistence | PostgreSQL 16, Spring Data JPA |
| Build | Maven |
| Local orchestration | Docker Compose |

## 2. Repository structure

```text
PatientFlow/
├── backend-springboot/
│   ├── src/main/java/com/patientflow/
│   │   ├── api/                 # Referral REST API
│   │   ├── model/               # Referral entity
│   │   ├── repository/          # Persistence repository
│   │   └── worker/              # Zeebe job workers
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── Dockerfile
│   └── pom.xml
├── bpmn-models/
│   ├── patient-referral-triage.bpmn
│   └── patient-consent-ehr-sync.bpmn
├── frontend-react/
│   ├── src/
│   ├── Dockerfile
│   └── package.json
├── docker-compose.yml
├── .env.example
└── README.md
```

## 3. Prerequisites

Install or prepare:

- Docker Desktop
- Java 21
- Maven
- Node.js and npm (for running the frontend outside Docker)
- Camunda 8.9.16 Run, or a compatible self-managed Camunda 8 environment

For the local Camunda Run setup used during development, the expected endpoints are:

| Service | Address |
|---|---|
| Camunda REST API | `http://localhost:8081` |
| Zeebe gateway (gRPC) | `localhost:26500` |
| PatientFlow backend | `http://localhost:8082` |
| PatientFlow frontend | `http://localhost:5173` |
| PostgreSQL | `localhost:5432` |

The Docker Compose backend connects to Camunda on the host using `host.docker.internal`. If your Camunda installation uses different ports or a remote cluster, update the corresponding environment variables.

## 4. Configuration

The backend reads the following environment variables. The defaults below are intended for a local academic demo.

| Variable | Default | Purpose |
|---|---|---|
| `CAMUNDA_CLIENT_GRPC_ADDRESS` | `http://localhost:26500` when running backend on host; Compose uses `http://host.docker.internal:26500` | Zeebe gateway |
| `CAMUNDA_CLIENT_REST_ADDRESS` | `http://localhost:8081` when running backend on host; Compose uses `http://host.docker.internal:8081` | Camunda REST API |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/patientflow` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `patientflow` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | `patientflow_dev` | Local development password |

`docker-compose.yml` supplies container-specific database and Camunda addresses. The `.env.example` file is a template, not a credentials file. Do not commit real passwords, tokens, or client secrets.

The local Camunda configuration uses `auth.method: none`. This is suitable only for an isolated local demonstration environment. Configure authentication and secure secrets before connecting to any shared or production environment.

## 5. Start the application with Docker Compose

1. Start Camunda 8 Run and confirm that its REST and Zeebe endpoints are available.
2. Open a terminal in the repository root.
3. Build and start PatientFlow:

```bash
docker compose up --build
```

Compose starts PostgreSQL, the Spring Boot backend, and the React frontend. The Camunda engine is run separately and is not included in this Compose stack.

Open:

- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8082`
- Backend health endpoint: `http://localhost:8082/actuator/health`

To stop the containers:

```bash
docker compose down
```

To stop containers and delete the persisted PostgreSQL volume (this removes local demo data):

```bash
docker compose down -v
```

## 6. Run components separately (development)

### PostgreSQL

Start only the database service from the repository root:

```bash
docker compose up -d postgres
```

### Backend

In a second terminal:

```bash
cd backend-springboot
mvn spring-boot:run
```

The backend listens on port `8082`. When running directly on the host, the default database URL is `jdbc:postgresql://localhost:5432/patientflow`, and the default Camunda endpoints are `localhost:26500` and `localhost:8081`.

### Frontend

In another terminal:

```bash
cd frontend-react
npm install
npm run dev
```

Open `http://localhost:5173`.

## 7. BPMN workflow design

### Main process: `patient-referral-triage`

The main process demonstrates the referral lifecycle:

1. A referral is submitted through the backend API and a process instance is started.
2. The `fetch-patient-vitals` service task retrieves synthetic observations.
3. If vitals cannot be retrieved, the `VITALS_UNAVAILABLE` boundary error path routes execution to `manual-vitals-review`.
4. An exclusive gateway evaluates `criticalResult`.
5. Critical cases follow the `emergency-clinical-escalation` path. The demo worker records a simulated escalation result.
6. The routine path coordinates specialist consultations using a parallel multi-instance activity over the `specialists` collection.
7. A four-hour timer boundary event is modelled for specialist SLA escalation.
8. Diagnostic reports are collected using a sequential multi-instance activity over `diagnosticTypes`.
9. A Call Activity invokes the reusable `patient-consent-ehr-sync` process, passing the referral identifier and consent value.

### Child process: `patient-consent-ehr-sync`

The reusable subprocess validates patient consent before attempting the mock EHR synchronization:

- If consent is given, the process continues to `sync-ehr-record`.
- If consent is not given, the `CONSENT_NOT_GIVEN` BPMN error is caught by the boundary error event and the process follows the consent-denied end path. The EHR synchronization task is skipped.

### BPMN concepts demonstrated

- Service tasks and worker-based automation
- Exclusive gateways and conditional sequence flows
- Error boundary events and recovery paths
- Parallel and sequential multi-instance activities
- Timer boundary event for SLA handling
- Call Activity and reusable subprocess
- Process variables and input/output mappings

## 8. Zeebe job workers

The backend registers workers for these job types:

| Job type | Demonstration responsibility |
|---|---|
| `fetch-patient-vitals` | Supplies synthetic patient observations |
| `manual-vitals-review` | Simulates review when vitals retrieval fails |
| `emergency-clinical-escalation` | Records a simulated critical-result escalation |
| `specialist-consultation` | Simulates a specialist consultation result |
| `diagnostic-report-collection` | Simulates collection of a diagnostic report |
| `specialist-sla-escalation` | Records a simulated overdue-work escalation |
| `validate-patient-consent` | Validates the consent process variable |
| `sync-ehr-record` | Simulates an EHR synchronization action |

These workers demonstrate workflow orchestration. Specialist consultations, escalations, diagnostic reports, and EHR synchronization are not real external actions or human approvals.

## 9. REST API

### Create a referral

```http
POST http://localhost:8082/api/referrals
Content-Type: application/json
```

Example request:

```json
{
  "patientName": "Alex Morgan",
  "age": 52,
  "reason": "Recurring chest discomfort",
  "specialists": ["Cardiology", "Radiology", "Dietetics"],
  "diagnosticTypes": ["Blood Test", "Imaging", "ECG"],
  "consentGiven": true,
  "simulateCriticalResult": false,
  "simulateVitalsUnavailable": false
}
```

The backend persists the referral and starts the `patient-referral-triage` process when Camunda is reachable.

### Example scenario flags

| Scenario | Request value | Expected workflow behaviour |
|---|---|---|
| Routine referral | `simulateCriticalResult: false`, `simulateVitalsUnavailable: false`, `consentGiven: true` | Routine referral path |
| Critical result | `simulateCriticalResult: true` | Simulated emergency escalation path |
| Vitals unavailable | `simulateVitalsUnavailable: true` | Error boundary path and manual vitals review |
| Consent denied | `consentGiven: false` | Consent-denied path; mock EHR sync skipped |

The application also exposes referral retrieval/status functionality through its API for the dashboard. Refer to `ReferralController.java` for the current route definitions.

## 10. Process variables

The workflow uses variables including:

- `referralId` — referral identifier passed to the child process
- `patientName`, `age`, and `reason` — synthetic referral details
- `specialists` — collection used by the parallel specialist activity
- `diagnosticTypes` — collection used by the sequential diagnostic activity
- `consentGiven` — consent decision passed to the child process
- `criticalResult` — result used by the critical/routine gateway
- `simulateCriticalResult` — demo input for the critical-result scenario
- `simulateVitalsUnavailable` — demo input for the vitals recovery scenario

Worker-generated variables include status/result fields for vitals, manual review, escalation, specialist consultation, diagnostic collection, consent validation, and mock EHR synchronization.

## 11. Demonstration and verification

The following scenarios were exercised during development:

1. Routine referral workflow.
2. Critical-result escalation path.
3. Vitals-unavailable error recovery and manual review.
4. Consent-denied handling through the child process.

The specialist consultation and escalation outcomes are simulated. The four-hour specialist SLA timer is modelled in BPMN; do not claim that a timer-expiry test was completed unless it has been separately run and verified in Operate.

For a demonstration, show the referral submission in the frontend, the resulting process instance in Camunda Operate, and the relevant BPMN path and process variables. Use synthetic patient names and values only.

## 12. Limitations and future extensions

- No real FHIR, HL7, hospital EHR, or clinical data integration is implemented.
- Specialist consultations and diagnostic report collection are simulated service-worker activities, not real clinician tasks.
- Escalation and SLA actions are simulated and do not contact emergency services or staff.
- The prototype is not clinically validated and must not be used in a healthcare setting.
- Production deployment would require authentication, authorization, secret management, audit controls, monitoring, resilient integrations, and clinical/safety validation.

## 13. License and academic use

This repository is intended for academic demonstration and evaluation. Add the license and attribution required by your institution or project team before redistributing it.
