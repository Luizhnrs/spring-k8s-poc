# KubeCTL-Test

Monorepo de microsserviços Java com autenticação JWT, analytics de tickets e notificações por email. Deploy em Kubernetes (Kind) ou Docker Compose.

---

## Microsserviços

| Serviço | Porta | Tecnologia | Descrição |
|---------|-------|------------|-----------|
| **POC** | `8080` | Spring Boot 4.0.6 | Autenticação JWT + Proxy para Tickets/Notificações |
| **Tickets Service** | `8081` | Spring Boot 4.0.6 | Analytics de tickets (contadores por evento) |
| **Notification Service** | `8082` | Spring Boot 4.0.6 | Notificações por email |

---

## Estrutura

```
KubeCTL-Test/
├── apps/                           # Código fonte
│   ├── poc/                        # Auth JWT + REST Clients
│   ├── tickets-service/            # Analytics de Tickets
│   └── notification-service/       # Notificações Email
├── k8s/                            # Manifestos Kubernetes
│   ├── poc/                        # POC + PostgreSQL + Kafka/ZK
│   ├── tickets-service/            # Tickets + PostgreSQL
│   └── notification-service/       # Notification Service
├── infra/                          # Docker Compose
└── README.md
```

---

## Endpoints para Teste (Insomnia / Postman)

### POC — `http://localhost:8080`

| Método | Rota | Body (JSON) | Descrição |
|--------|------|-------------|-----------|
| `GET` | `/hello` | — | Health check |
| `POST` | `/api/auth/register` | `{"username":"...","password":"...","email":"..."}` | Cadastro → dispara NOTIFICACAO + email |
| `POST` | `/api/auth/login` | `{"username":"...","password":"..."}` | Login → dispara AUDITORIA + retorna JWT |
| `POST` | `/api/auth/validate` | Header: `Authorization: Bearer <token>` | Validar token JWT |
| `GET` | `/api/tickets/stats` | — | Todos os tickets (proxy) |
| `GET` | `/api/tickets/{type}` | — | Ticket por tipo (proxy) |
| `GET` | `/api/tickets/total` | — | Total de tickets (proxy) |
| `POST` | `/api/tickets/increment` | `{"type":"PRIORIDADE"}` | Incrementar ticket (proxy) |

### Notification Service — `http://localhost:8082`

| Método | Rota | Body (JSON) | Descrição |
|--------|------|-------------|-----------|
| `POST` | `/api/notifications/email` | `{"email":"...","username":"..."}` | Enviar email de boas-vindas |

### Tickets Service (direto) — `http://localhost:8081`

| Método | Rota | Body (JSON) | Descrição |
|--------|------|-------------|-----------|
| `GET` | `/api/tickets/stats` | — | Todos os tickets |
| `GET` | `/api/tickets/{type}` | — | Ticket por tipo |
| `POST` | `/api/tickets/increment` | `{"type":"NOTIFICACAO"}` | Incrementar contador |

---

## Fluxo Principal

```
Register ──→ POC ──→ Tickets Service (NOTIFICACAO++)
                 ──→ Notification Service (Email boas-vindas)

Login ────→ POC ──→ Tickets Service (AUDITORIA++)
                 ──→ Retorna JWT Token
```

---

## LocalStack (SNS/SQS)

Os listeners assíncronos usam LocalStack para simular AWS.

| Serviço | Tópico SNS | Fila SQS | Listener |
|---------|-----------|----------|----------|
| Tickets Service | `arn:aws:sns:...:SNS-Topic` | `ticket-events-queue` | `SnsTicketListener` |
| Notification Service | `arn:aws:sns:...:email-notification` | `email-notification-queue` | `SnsEmailListener` |

**Endpoint:** `https://localhost.localstack.cloud:4566` (região `us-east-1`)

**Ativar/Desativar:** `app.sns.enabled=true/false` (em `application.properties`)

---

## Como Rodar

### Local (port-forward + Maven)

```bash
# Terminal 1: PostgreSQL do POC
kubectl port-forward svc/postgres-service 5433:5432

# Terminal 2: Tickets Service
kubectl port-forward svc/tickets-service 8081:8081

# Terminal 3: Notification Service (opcional, via Maven)
cd apps/notification-service && .\mvnw.cmd spring-boot:run

# Terminal 4: POC
cd apps/poc && .\mvnw.cmd spring-boot:run
```

### Docker Compose

```bash
docker compose -f infra/docker-compose.yml up --build
```

---

## Tecnologias

**Java 21** · **Spring Boot 4.0.6 / 3.4.4** · **Spring Security** · **JWT (JJWT 0.11.5)** · **BCrypt** · **PostgreSQL 15** · **HikariCP** · **AWS SDK (SNS/SQS 2.29.52)** · **LocalStack** · **Docker** · **Kind** · **Lombok**
