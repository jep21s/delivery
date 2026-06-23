# Демо проект к курсу "Domain Driven Design и Clean Architecture на языке Java"
📚 Подробнее о курсе: [microarch.ru/courses/ddd/languages/java](https://microarch.ru/courses/ddd/languages/java?utm_source=gitlab&utm_medium=repository)

---

## Условия использования

Вы можете использовать и модифицировать данный код **в образовательных целях**, при условии сохранения ссылки на курс и оригинального источника.

---

# Запросы к БД
```
SELECT * public.assignments;
SELECT * FROM public.couriers;
SELECT * FROM public.orders;
SELECT * public.outbox;
```

# Очистка БД (все кроме справочников)
```
DELETE FROM public.assignments;
DELETE FROM public.couriers;
DELETE FROM public.orders;

DELETE FROM public.outbox;
```

# Сборка проекта (генерация HTTP-сервера из OpenAPI, gRPC-клиента и Kafka-событий из Protobuf)
```
./gradlew clean build
```

## Лицензия

Код распространяется под лицензией [MIT](./LICENSE).  
© 2025 microarch.ru