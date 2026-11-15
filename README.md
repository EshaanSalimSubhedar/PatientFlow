# PatientFlow — Multidisciplinary Patient Referral & Triage

Academic prototype inspired by the NHS multidisciplinary referral workflow. A GP submits a referral, the workflow retrieves mock vitals, routes critical cases for emergency escalation, coordinates specialist reviews, and invokes a reusable consent/EHR synchronization process.

> Safety: uses synthetic data only. Triage thresholds are demonstration rules, not clinical guidance. This software must not be used for diagnosis or treatment.

## Stack
- React + Vite
- Java 21, Spring Boot 3.3
- Camunda 8 / Zeebe (local Camunda 8 Run or a configured cluster)
- H2 (development persistence)
- Docker Compose

## Repository layout
- `bpmn-models/` — main referral process and reusable consent/EHR child process
- `backend-springboot/` — REST API, H2 entities/repository, Zeebe workers
- `frontend-react/` — referral form, dashboard, tasklist
- `docker-compose.yml` — PostgreSQL + backend + frontend (Camunda engine is configured separately)

## Run locally
### 1. Camunda
Start Camunda 8 Run (or a Camunda 8 cluster) and make sure the Zeebe gateway is reachable at `localhost:26500`. Deploy both BPMN files from `bpmn-models/` using Modeler/Operate.

### 2. Backend
```bash
cd backend-springboot
mvn spring-boot:run
```
Default API: `http://localhost:8080`. Configure Zeebe connection with `ZEEBE_ADDRESS` and `ZEEBE_CLIENT_ID` / `ZEEBE_CLIENT_SECRET` for a secured cluster.

### 3. Frontend
```bash
cd frontend-react
npm install
npm run dev
```
Open `http://localhost:5173`.

### 4. Docker
```bash
docker compose up --build
```
The compose file starts PostgreSQL, backend, and frontend. Configure the Zeebe gateway address for the environment in which Docker runs; `localhost` inside a container refers to that container, not the host.

## Workflow outline
1. `POST /api/referrals` persists the referral and starts `patient-referral-triage` when Zeebe is connected.
2. `fetch-patient-vitals` retrieves synthetic vitals from the mock integration service.
3. If `criticalResult` is true, the process executes emergency escalation.
4. `specialist-review` runs as a parallel multi-instance activity over the requested departments.
5. The process invokes `patient-consent-ehr-sync` through a Call Activity.
6. The dashboard polls the API for referral status and specialist work items.

## Sample request
```json
{
  "patientName": "Alex Morgan",
  "age": 52,
  "reason": "Recurring chest discomfort",
  "specialists": ["Cardiology", "Radiology", "Dietetics"],
  "consentGiven": true
}
```

## FEEL expression dictionary
- `=criticalResult = true` — critical-result branch.
- `=criticalResult = false` — routine branch.
- `=specialists` — multi-instance input collection.
- `=specialist` — current multi-instance element.
- `=referralId` — referral identifier passed into the consent/EHR child process.

## Notes / implementation status
This repository is a working starter implementation. Verify the Camunda SDK version and cluster authentication settings for your installation. Deploy and test the BPMN models against your exact Camunda 8 version before claiming successful engine execution. The mock vitals worker deliberately uses synthetic data. Email/SMS and production FHIR/HL7 connectivity are extension points; no real patient system is contacted.
