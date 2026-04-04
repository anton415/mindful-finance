# 🌿 Mindful Finance

[![Built with Codex](https://img.shields.io/badge/Built%20with-Codex-0A66C2?logo=openai&logoColor=white)](https://openai.com/codex/)
[![CI](https://github.com/anton415/mindful-finance/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/anton415/mindful-finance/actions/workflows/ci.yml)
[![Architecture Guard](https://github.com/anton415/mindful-finance/actions/workflows/architecture-guard.yml/badge.svg?branch=main)](https://github.com/anton415/mindful-finance/actions/workflows/architecture-guard.yml)
[![CodeQL](https://github.com/anton415/mindful-finance/actions/workflows/codeql.yml/badge.svg?branch=main)](https://github.com/anton415/mindful-finance/actions/workflows/codeql.yml)
[![Dependency Review](https://img.shields.io/badge/Dependency%20Review-PR%20only-6e5494?logo=github&logoColor=white)](https://github.com/anton415/mindful-finance/actions/workflows/dependency-review.yml)
[![Code Style: Spotless](https://img.shields.io/badge/Code%20Style-Spotless-2F80ED)](https://github.com/diffplug/spotless)
[![Code Style: Prettier](https://img.shields.io/badge/Code%20Style-Prettier-F7B93E?logo=prettier&logoColor=1A2B34)](https://prettier.io/)
[![ESLint Safe Profile](https://img.shields.io/badge/ESLint-safe%20profile-4B32C3?logo=eslint&logoColor=white)](./frontend/eslint.config.js)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.x](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React 19](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![TypeScript 5.x](https://img.shields.io/badge/TypeScript-5.x-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Tailwind CSS 4.x](https://img.shields.io/badge/Tailwind%20CSS-4.x-06B6D4?logo=tailwindcss&logoColor=white)](https://tailwindcss.com/)

**Подход Calm Architect к финансовой свободе.**

## 🎯 Видение
Большинство финансовых приложений специально удерживают внимание мигающими цифрами и постоянными уведомлениями. **Mindful Finance** создаётся для долгосрочной стратегии и конечного результата. В приоритете — целостность данных (Truth) и психологическое спокойствие (Calm).

## 🛠 Технологический стек
- **Backend:** Java 21, Spring Boot 3.x, PostgreSQL.
- **Frontend:** React, Vite, TypeScript, Tailwind CSS.
- **Архитектура:** Hexagonal / Clean Architecture.

## ✅ Запуск тестов
Требования: Java 21 и Maven.

Из корня репозитория:
`mvn -f backend/pom.xml test`

## 🐘 Локальный запуск с PostgreSQL (БД + API + Frontend)
Требования: Java 21, Maven, Docker, Node.js, npm и `make`.

Запуск всего локального стека одной командой из корня репозитория:
```bash
make dev
```

Что делает `make dev`:
- подготавливает backend-модули для запуска API без отдельного ручного `mvn install`;
- поднимает PostgreSQL из `backend/docker-compose.yml`;
- ждёт healthcheck контейнера;
- запускает Spring Boot API с профилем `postgres`;
- запускает Vite frontend;
- при первом запуске или изменении `frontend/package-lock.json` автоматически выполняет `npm ci`.

После старта:
- API доступен на `http://localhost:8080`;
- health-check: `curl http://localhost:8080/health`;
- ожидаемый ответ:
```json
{"status":"ok"}
```
- frontend доступен на `http://localhost:5173`.

Остановка приложения:
- `Ctrl+C` завершает backend и frontend;
- PostgreSQL остаётся запущенным для быстрого следующего старта.

Остановить локальный PostgreSQL:
```bash
make down
```

`make down` останавливает контейнер, но не удаляет локальные данные. Postgres хранит данные в named volume `mindful-finance-postgres-data`, поэтому база переживает `make down` и следующий `make dev`.

Чтобы намеренно сбросить локальную dev-базу и начать с пустого состояния:
```bash
docker compose -f backend/docker-compose.yml down -v
```

Собрать backend и frontend одной командой:
```bash
make build
```

Локальная база данных доступна на `localhost:55432`, чтобы не конфликтовать с PostgreSQL на `5432`.

По умолчанию frontend использует `VITE_API_BASE_URL=/api`, а Vite-прокси перенаправляет запросы на `http://localhost:8080`.

При необходимости параметры подключения к БД можно переопределить переменными окружения:
- `MINDFUL_FINANCE_DB_URL`
- `MINDFUL_FINANCE_DB_USERNAME`
- `MINDFUL_FINANCE_DB_PASSWORD`

## ☕ Backend-only запуск для разработки

Если нужен только API без frontend, используй отдельный backend runtime из корня репозитория:
```bash
make backend-dev
```

Этот режим:
- поднимает тот же локальный PostgreSQL;
- ждёт healthcheck контейнера;
- запускает Spring Boot с `SPRING_PROFILES_ACTIVE=postgres`;
- использует те же `MINDFUL_FINANCE_DB_*`, что и `make dev`.

Для VS Code в репозитории сохранён launch config `Mindful Finance API (postgres)` и pre-launch task, который поднимает локальный PostgreSQL перед стартом приложения.

Ручной локальный backend-запуск без профиля `postgres` считается вспомогательным in-memory режимом, а не основным dev-runtime.

## 📘 Документы по workflow
- Для разработчика: [`docs/product/05-developer-local-workflow.md`](docs/product/05-developer-local-workflow.md)
- Для пользователя: [`docs/product/06-user-personal-finance-review-flow.md`](docs/product/06-user-personal-finance-review-flow.md)

## 🧘 Принципы системы
1. **Сначала Truth:** никаких float/double для денег. Только `BigDecimal`.
2. **Контекст важнее шума:** акцент на долгосрочных трендах, а не на дневных колебаниях.
3. **Calm UI:** без навязчивых уведомлений; информация показывается по запросу.
4. **Логика важнее импульса:** подход как в шахматах — оценивать позицию, а не реагировать на последний ход.

## 🗺 Планирование
Актуальные milestones и задачи ведутся только в GitHub:

- Milestones: https://github.com/anton415/mindful-finance/milestones
- Issues: https://github.com/anton415/mindful-finance/issues
GitHub milestones и tracking issues являются единственным источником roadmap truth.
