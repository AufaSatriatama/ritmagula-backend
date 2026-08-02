# Mobile App Starter

Template monorepo untuk aplikasi mobile dengan **React Native (Expo + TypeScript)**, **Java Spring Boot**, dan **PostgreSQL**. Fitur contoh yang tersedia: registrasi/login JWT, penyimpanan sesi, dan CRUD task per pengguna.

## Struktur

```text
.
├── mobile/       # Expo React Native
├── backend/      # Spring Boot REST API
├── compose.yaml  # PostgreSQL + API
└── .env.example
```

## Prasyarat

- Node.js 20+ dan npm
- Java 21 dan Maven 3.9+
- PostgreSQL 16+, atau Docker Desktop untuk menjalankan database
- Expo Go pada perangkat, atau Android/iOS simulator

## Menjalankan secara lokal

### 1. Database

Cara termudah adalah menjalankan PostgreSQL melalui Docker:

```bash
docker compose up -d postgres
```

Tanpa Docker, buat database dan user PostgreSQL sesuai nilai default berikut:

```text
database: appdb
username: appuser
password: appsecret
```

Flyway membuat tabel secara otomatis ketika backend mulai.

### 2. Backend

```bash
cd backend
mvn spring-boot:run
```

API tersedia di `http://localhost:8080/api`, dan health check di `http://localhost:8080/actuator/health`.

Untuk kredensial database yang berbeda, atur `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, dan `JWT_SECRET`. Rahasia JWT produksi wajib berupa nilai acak minimal 32 byte.

### 3. Mobile

```bash
cd mobile
copy .env.example .env
npm install
npm start
```

Sesuaikan `EXPO_PUBLIC_API_URL` di `mobile/.env` menurut target:

- Android emulator: `http://10.0.2.2:8080/api`
- iOS simulator atau web: `http://localhost:8080/api`
- Perangkat fisik: `http://<IP-LAN-komputer>:8080/api`

Komputer dan perangkat fisik harus berada pada jaringan yang sama. Setelah mengganti `.env`, restart Expo.

## Menjalankan seluruh backend dengan Docker

Salin `.env.example` menjadi `.env`, ganti semua secret, lalu:

```bash
docker compose up --build
```

## Endpoint contoh

| Method | Endpoint | Akses | Fungsi |
|---|---|---|---|
| `POST` | `/api/auth/register` | Publik | Registrasi dan memperoleh token |
| `POST` | `/api/auth/login` | Publik | Login dan memperoleh token |
| `GET` | `/api/tasks` | Bearer token | Daftar task pengguna |
| `POST` | `/api/tasks` | Bearer token | Membuat task |
| `PATCH` | `/api/tasks/{id}` | Bearer token | Mengubah judul/status |
| `DELETE` | `/api/tasks/{id}` | Bearer token | Menghapus task |

## Pemeriksaan

```bash
cd backend && mvn test
cd mobile && npm run typecheck
```

## Pengembangan berikutnya

- Ganti package/bundle identifier pada `mobile/app.json`.
- Tambahkan migration baru di `backend/src/main/resources/db/migration`; jangan mengubah migration yang sudah pernah dijalankan.
- Pisahkan konfigurasi development/staging/production dan simpan secret di secret manager.
- Tambahkan refresh token, rate limiting, observability, dan pengujian integrasi sebelum digunakan di produksi.
