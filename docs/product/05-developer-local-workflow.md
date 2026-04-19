# Guide-Dev. Локальный workflow разработчика

**Статус:** рабочий документ  
**Назначение документа:** зафиксировать единый локальный workflow разработки и проверки изменений в Mindful Finance.  
**Связь с Техническим заданием:** документ поддерживает инженерный контур выполнения roadmap-задач и closure-loop по milestone.  
**Источник в репозитории:** `docs/product/05-developer-local-workflow.md`

## 1. Назначение и границы

Документ описывает минимальный локальный путь разработчика от изменения к проверяемому результату без расширения продуктового scope. Основной принцип состоит в том, что каждое изменение должно проходить через проверяемый checkpoint и фиксироваться в issue или milestone как источник roadmap truth.

## 2. Требования к окружению

Для локальной разработки требуются Java 21+, Maven, Docker, Node.js, npm и `make`. Перед началом работы необходимо убедиться, что локальный репозиторий синхронизирован с `main`, а рабочее дерево не содержит нерелевантных незакоммиченных изменений.

Ручной локальный runtime backend должен использовать профиль `postgres` и общий local datasource через `MINDFUL_FINANCE_DB_URL`, `MINDFUL_FINANCE_DB_USERNAME`, `MINDFUL_FINANCE_DB_PASSWORD`. In-memory режим допустим только как вспомогательный и не считается стандартным dev-запуском.

## 3. Базовый цикл разработки

1. Определить outcome задачи и зафиксировать done criteria в issue.
2. Сформулировать thin vertical slice с одним поведенческим инвариантом.
3. Внести минимальные изменения в код без изменения публичного API, если это не требуется scope задачи.
4. Добавить или обновить regression tests на изменяемую ветку поведения.
5. Прогнать checkpoint-команды и сохранить результат в заметках задачи.

## 4. Checkpoint-команды

Обязательный минимальный checkpoint для milestone closure:

- `mvn -f backend/pom.xml test`
- `npm -C frontend run build`

Рекомендуемый дополнительный quality-check:

- `npm -C frontend run lint`

Проверка локального runtime smoke:

1. `make dev`
2. `curl http://localhost:${MINDFUL_FINANCE_API_PORT:-8080}/health` (ожидаемый ответ: `{"status":"ok"}`)
3. открыть `http://localhost:${MINDFUL_FINANCE_FRONTEND_PORT:-5173}`
4. `make down`

Перед реальным запуском допускается безопасный dry-run orchestration через `make -n dev`.

Backend-only smoke для разработки с сохранением тех же данных:

1. `make backend-dev`
2. `curl http://localhost:${MINDFUL_FINANCE_API_PORT:-8080}/health` (ожидаемый ответ: `{"status":"ok"}`)
3. остановить процесс через `Ctrl+C`

Для backend-only пути безопасный dry-run выполняется через `make -n backend-dev`.

Если `8080` или `5173` заняты, локальный runtime должен запускаться через переопределение `MINDFUL_FINANCE_API_PORT` и/или `MINDFUL_FINANCE_FRONTEND_PORT`, а не через правку кода.

Локальный PostgreSQL хранит данные в persistent Docker volume и должен переживать `make down`. Если нужен чистый старт базы, он выполняется явно через `docker compose -f backend/docker-compose.yml down -v`.

## 5. Проверка регрессий и багов

Каждый кодовый срез должен содержать минимум один тест на позитивный сценарий и один тест на критичную негативную ветку, если такая ветка существует. Для financial domain недопустимы изменения, которые переводят money-логику с `BigDecimal` на `float`/`double` или смешивают domain-правила с инфраструктурными деталями.

## 6. Синхронизация документации и roadmap-truth

Изменения в документации сначала вносятся в `docs/product` или `docs/domain`, затем зеркалируются в GitHub Wiki. Все важные решения, которые влияют на milestone completion, фиксируются в соответствующем issue или tracking issue.

## 7. Критерий завершения инженерного среза

Срез считается завершенным, когда:

- поведение подтверждено тестами и checkpoint-командами;
- локальный runtime smoke пройден;
- документация и wiki синхронизированы (если были изменения);
- в issue оставлен короткий итог: что изменено, что проверено, что осталось.
