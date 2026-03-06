# 🌿 Mindful Finance

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
Требования: Java 21, Maven, Docker, Node.js и npm.

1. Запустите PostgreSQL:
```bash
docker compose -f backend/docker-compose.yml up -d
docker compose -f backend/docker-compose.yml ps
```

2. Если запускаете модуль `api` отдельно, один раз установите backend-модули из корня репозитория:
```bash
mvn -f backend/pom.xml install
```

3. Запустите API с профилем `postgres`:
```bash
mvn -f backend/api/pom.xml -Dspring-boot.run.profiles=postgres spring-boot:run
```

4. Проверьте, что API поднялся:
```bash
curl http://localhost:8080/health
```
Ожидаемый ответ:
```json
{"status":"ok"}
```

5. В отдельном терминале запустите frontend:
```bash
cd frontend
npm install
npm run dev
```

Локальная база данных доступна на `localhost:55432`, чтобы не конфликтовать с PostgreSQL на `5432`.

По умолчанию frontend использует `VITE_API_BASE_URL=/api`, а Vite-прокси перенаправляет запросы на `http://localhost:8080`.

При необходимости параметры подключения к БД можно переопределить переменными окружения:
- `MINDFUL_FINANCE_DB_URL`
- `MINDFUL_FINANCE_DB_USERNAME`
- `MINDFUL_FINANCE_DB_PASSWORD`

Остановить локальный PostgreSQL:
```bash
docker compose -f backend/docker-compose.yml down
```

## 🧘 Принципы системы
1. **Сначала Truth:** никаких float/double для денег. Только `BigDecimal`.
2. **Контекст важнее шума:** акцент на долгосрочных трендах, а не на дневных колебаниях.
3. **Calm UI:** без навязчивых уведомлений; информация показывается по запросу.
4. **Логика важнее импульса:** подход как в шахматах — оценивать позицию, а не реагировать на последний ход.

## 🗺 Планирование
Актуальные milestones и задачи ведутся в GitHub:

- Milestones: https://github.com/anton415/mindful-finance/milestones
- Issues: https://github.com/anton415/mindful-finance/issues

Спеки этапов проекта лежат в `docs/milestones/`.
