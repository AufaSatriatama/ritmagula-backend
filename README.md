# RitmaGula Backend

Spring Boot application backend and PostgreSQL schema for the RitmaGula research-only mobile product. This repository consumes the protected Python model services through typed HTTP clients; it does not contain or modify model code or weights.

## Stack

- Java 21 and Spring Boot 4.1
- Maven Wrapper
- PostgreSQL with Flyway
- Spring Data JPA, Validation, MVC, and Actuator

## Local configuration

Copy `.env.example` values into your local environment. Never commit real credentials.

Create the local PostgreSQL role and empty `ritmagula` database first. Flyway creates and validates the application schema when Spring starts. Replace port `5432` below when the local service uses another port such as `5433`.

```powershell
$env:RITMAGULA_DATABASE_URL = "jdbc:postgresql://127.0.0.1:5432/ritmagula"
$env:RITMAGULA_DATABASE_USER = "ritmagula"
$env:RITMAGULA_DATABASE_PASSWORD = "local-only-password"
$env:RITMAGULA_BACKEND_ADDRESS = "127.0.0.1"
$env:RITMAGULA_RISK_BASE_URL = "http://127.0.0.1:8000"
$env:RITMAGULA_FOOD_BASE_URL = "http://127.0.0.1:8001"
$env:RITMAGULA_ALLOWED_ORIGINS = "http://127.0.0.1:8081,http://localhost:8081"
```

## Run and verify

```powershell
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run
```

The application listens on `http://127.0.0.1:8080` by default. The loopback binding is intentional for the local fictional-data prototype; do not change it to a LAN/public address without designing authentication and a real-data policy. The P0 API is under `/api/v1`; actuator health is `/actuator/health`.

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
```

Key local endpoints:

- `POST /api/v1/demo-sessions` creates a consent-gated fictional session.
- `PUT /api/v1/demo-sessions/{sessionId}/profile` accepts the v2.1 profile, including optional `waistCircumferenceCm` (50-200 cm).
- `PUT /api/v1/demo-sessions/{sessionId}/days/{date}/activity` and `POST .../meals` collect confirmed 14-day inputs.
- `GET /api/v1/demo-sessions/{sessionId}/days/{date}/meals` lists only persisted meals for that session and day.
- `GET /api/v1/demo-sessions/{sessionId}/timeline` calculates the two independent 7-day quality windows.
- `POST /api/v1/demo-sessions/{sessionId}/screenings` calls Risk v2.1 and preserves `ok`, `abstained`, timeout, and unavailable states.
- `POST /api/v1/demo-sessions/{sessionId}/food-analyses` accepts JPEG/PNG/WebP multipart uploads up to 10 MB and never persists the raw image.
- `POST /api/v1/demo-sessions/{sessionId}/days/{date}/food-confirmations` requires corrected label, portion, modifiers, and explicit confirmation before persisting the TKPI/analog journal meal.
- `DELETE /api/v1/demo-sessions/{sessionId}` physically removes the session cascade and returns a `SESSION_RESET` JSON envelope.

All success and failure paths use the shared JSON envelope. Validation/malformed JSON return 422, missing API resources return 404, unexpected failures return 500, and reset returns 200; a real embedded HTTP-server regression test covers these responses in addition to MockMvc tests.

Expired sessions are purged on a configurable schedule. Defaults are a 30-second initial delay and a five-minute interval; override them with `RITMAGULA_RETENTION_CLEANUP_INITIAL_DELAY_MS` and `RITMAGULA_RETENTION_CLEANUP_DELAY_MS`.

Database migration sources and their deterministic fictional fixture checks remain under `database/`. Flyway also loads the application migration from `src/main/resources/db/migration/`.

## Safety boundary

- Research screening only; never diagnosis or treatment.
- React calls this backend, not the protected model APIs directly.
- Model credentials remain server-side.
- No fabricated result is returned when a model is unavailable.
- No raw food image, model request body, probability payload, name, email, password, or API key is stored by the P0 schema.
- Every current session is `FICTIONAL_DEMO`; clients must not solicit or submit real personal/health data.
- Food analysis suggestions are not persisted. Only a user-confirmed meal plus non-image request/catalog/model provenance enters the journal, and an analysis cannot be confirmed twice.
- The current protected checkout is expected to report both model services as `not_ready` until approved checksum-matching artifacts are supplied.

## How to Run the App (Sorry for still using our path)

## 0. Setup model artifact — sekali saja

Jalankan di PowerShell biasa. Ini membuat runtime di luar ketiga repository, jadi model repository tidak berubah.

powershell
$ritmaRoot = "C:\Users\Mahendra's\OneDrive\Documents\Double Shot Espresso\Datathon\SemiFinal"
$ritmaRuntime = "$ritmaRoot\.runtime\model-artifacts"

New-Item -ItemType Directory -Force `
  -Path "$ritmaRuntime\risk_v2", "$ritmaRuntime\food\experiments" |
  Out-Null

Copy-Item -LiteralPath `
  "$ritmaRoot\ritmagula-model\artifacts\risk_v2\manifest.json" `
  -Destination "$ritmaRuntime\risk_v2\manifest.json" `
  -Force

Copy-Item -LiteralPath `
  "C:\Users\Mahendra's\Downloads\stable_two_stage_logistic.onnx" `
  -Destination "$ritmaRuntime\risk_v2\stable_two_stage_logistic.onnx" `
  -Force

Copy-Item -LiteralPath `
  "$ritmaRoot\ritmagula-model\artifacts\food\manifest.json" `
  -Destination "$ritmaRuntime\food\manifest.json" `
  -Force

Copy-Item -LiteralPath `
  "C:\Users\Mahendra's\Downloads\recognizer.onnx" `
  -Destination "$ritmaRuntime\food\recognizer.onnx" `
  -Force

Copy-Item -LiteralPath `
  "C:\Users\Mahendra's\Downloads\segformer_b0.onnx" `
  -Destination "$ritmaRuntime\food\experiments\segformer_b0.onnx" `
  -Force


Untuk mvp_food_profiles.json, gunakan blok ini agar line ending-nya sesuai checksum manifest:

powershell
$ritmaProfileSource = "C:\Users\Mahendra's\OneDrive\Documents\Double Shot Espresso\Datathon\SemiFinal\ritmagula-model\artifacts\food\mvp_food_profiles.json"
$ritmaProfileTarget = "C:\Users\Mahendra's\OneDrive\Documents\Double Shot Espresso\Datathon\SemiFinal\.runtime\model-artifacts\food\mvp_food_profiles.json"

$ritmaProfileText = [System.IO.File]::ReadAllText($ritmaProfileSource)
$ritmaProfileText = $ritmaProfileText.Replace("`r`n", "`n")

[System.IO.File]::WriteAllText(
    $ritmaProfileTarget,
    $ritmaProfileText,
    [System.Text.UTF8Encoding]::new($false)
)


Verifikasi checksum:

powershell
Get-FileHash -Algorithm SHA256 -LiteralPath `
  "C:\Users\Mahendra's\OneDrive\Documents\Double Shot Espresso\Datathon\SemiFinal\.runtime\model-artifacts\risk_v2\stable_two_stage_logistic.onnx"

Get-FileHash -Algorithm SHA256 -LiteralPath `
  "C:\Users\Mahendra's\OneDrive\Documents\Double Shot Espresso\Datathon\SemiFinal\.runtime\model-artifacts\food\experiments\segformer_b0.onnx"

Get-FileHash -Algorithm SHA256 -LiteralPath `
  "C:\Users\Mahendra's\OneDrive\Documents\Double Shot Espresso\Datathon\SemiFinal\.runtime\model-artifacts\food\recognizer.onnx"

Get-FileHash -Algorithm SHA256 -LiteralPath `
  "C:\Users\Mahendra's\OneDrive\Documents\Double Shot Espresso\Datathon\SemiFinal\.runtime\model-artifacts\food\mvp_food_profiles.json"


Expected hash:

text
stable_two_stage_logistic.onnx
bb35bd41cf4b68c709e01d3c53fa39648592c9348d9703e6579ac2bca3417e8f

segformer_b0.onnx
a4cab06f62b9f5ce0a440cd97dd527ec05f9218db75059dec655afa1a7023868

recognizer.onnx
d41d6766fb8b41ad9c7f94678c76a8158c61ed0c9c97dc51e36b16222b025595

mvp_food_profiles.json
3e75d9b14e3349bbb2774ba99da4594213c59cc7c90e1585c58c70f515aa6695


## 1. Setup Python — sekali saja

powershell
Set-Location -LiteralPath "C:\Users\Mahendra's\OneDrive\Documents\Double Shot Espresso\Datathon\SemiFinal\ritmagula-model"

python -m venv .venv

& ".\.venv\Scripts\python.exe" -m pip install --upgrade pip
& ".\.venv\Scripts\pip.exe" install -r requirements.txt


## 2. Pastikan PostgreSQL menyala

powershell
Get-Service -Name "postgresql-x64-18"


Di komputermu saat ini service tersebut sudah Running dan Automatic.

Kalau suatu saat statusnya Stopped, buka PowerShell sebagai Administrator:

powershell
Start-Service -Name "postgresql-x64-18"


Tes koneksi database:

powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" `
  -h 127.0.0.1 `
  -p 5433 `
  -U ritmagula `
  -d ritmagula `
  -c "SELECT current_database(), current_user;"


Masukkan password role ritmagula ketika diminta.

---

## 3. Terminal 1 — nyalakan Risk API

Buka PowerShell baru:

powershell
Set-Location -LiteralPath "C:\Users\Mahendra's\OneDrive\Documents\Double Shot Espresso\Datathon\SemiFinal\ritmagula-model"

$env:PYTHONPATH = "src"
$env:RITMAGULA_RISK_ARTIFACT_DIR = "C:\Users\Mahendra's\OneDrive\Documents\Double Shot Espresso\Datathon\SemiFinal\.runtime\model-artifacts\risk_v2"
$env:RITMAGULA_ALLOWED_ORIGINS = "http://127.0.0.1:8080,http://localhost:8080"

& ".\.venv\Scripts\python.exe" -m uvicorn ritmagula.api:app `
  --host 127.0.0.1 `
  --port 8000 `
  --no-access-log


Biarkan terminal ini menyala.

## 4. Terminal 2 — nyalakan Food API

Buka PowerShell baru:

powershell
Set-Location -LiteralPath "C:\Users\Mahendra's\OneDrive\Documents\Double Shot Espresso\Datathon\SemiFinal\ritmagula-model"

$env:PYTHONPATH = "src"
$env:RITMAGULA_FOOD_MODE = "mvp_assist"
$env:RITMAGULA_FOOD_ARTIFACT_DIR = "C:\Users\Mahendra's\OneDrive\Documents\Double Shot Espresso\Datathon\SemiFinal\.runtime\model-artifacts\food"
$env:RITMAGULA_ALLOWED_ORIGINS = "http://127.0.0.1:8080,http://localhost:8080"

& ".\.venv\Scripts\python.exe" -m uvicorn ritmagula.food_api:app `
  --host 127.0.0.1 `
  --port 8001 `
  --no-access-log


Biarkan terminal ini menyala.

## 5. Terminal 3 — cek kedua model API

powershell
Invoke-RestMethod http://127.0.0.1:8000/v2/health |
  ConvertTo-Json -Depth 10

Invoke-RestMethod http://127.0.0.1:8001/v2/health |
  ConvertTo-Json -Depth 10


Keduanya harus menunjukkan ready=true.

## 6. Terminal 4 — nyalakan backend

Buka PowerShell baru:

powershell
Set-Location -LiteralPath "C:\Users\Mahendra's\OneDrive\Documents\Double Shot Espresso\Datathon\SemiFinal\ritmagula-backend"

$ritmaDatabaseSecret = Read-Host "Masukkan password PostgreSQL user ritmagula" -AsSecureString

$env:RITMAGULA_DATABASE_PASSWORD = [System.Net.NetworkCredential]::new(
    "",
    $ritmaDatabaseSecret
).Password

$env:RITMAGULA_DATABASE_URL = "jdbc:postgresql://127.0.0.1:5433/ritmagula"
$env:RITMAGULA_DATABASE_USER = "ritmagula"

$env:RITMAGULA_BACKEND_ADDRESS = "127.0.0.1"
$env:RITMAGULA_BACKEND_PORT = "8080"
$env:RITMAGULA_RISK_BASE_URL = "http://127.0.0.1:8000"
$env:RITMAGULA_FOOD_BASE_URL = "http://127.0.0.1:8001"
$env:RITMAGULA_ALLOWED_ORIGINS = "http://127.0.0.1:8081,http://localhost:8081"
$env:RITMAGULA_API_KEY = ""

& ".\mvnw.cmd" spring-boot:run


Biarkan terminal ini menyala.

## 7. Terminal 5 — cek backend

powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health |
  ConvertTo-Json -Depth 10

Invoke-RestMethod http://127.0.0.1:8080/api/v1/system/readiness |
  ConvertTo-Json -Depth 10


Pastikan:

- aplikasi backend sehat;
- Risk tersedia;
- Food tersedia.

## 8. Terminal 6 — nyalakan frontend web hotfix

Buka PowerShell baru:

powershell
Set-Location -LiteralPath "C:\Users\Mahendra's\OneDrive\Documents\Double Shot Espresso\Datathon\SemiFinal\ritmagula-frontend"

git status --short --branch


Output branch seharusnya:

text
## hotfix...team-fork/hotfix


Simpan alamat backend:

powershell
Set-Content -LiteralPath ".env.local" `
  -Value "EXPO_PUBLIC_API_URL=http://127.0.0.1:8080"


Kemudian:

powershell
npm ci
npm run web


Buka browser:

text
http://localhost:8081


## Urutan setiap kali ingin menjalankan kembali

Setelah setup pertama selesai, cukup:

1. Pastikan PostgreSQL hidup.
2. Jalankan Terminal Risk API.
3. Jalankan Terminal Food API.
4. Pastikan keduanya ready=true.
5. Jalankan Spring Boot.
6. Pastikan readiness backend sehat.
7. Jalankan npm run web di frontend.
8. Buka http://localhost:8081.

Untuk menghentikan service, tekan Ctrl+C di setiap terminal.