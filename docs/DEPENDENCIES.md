# Direct Maven dependencies (M2-071)

Reviewed against `pom.xml`. Transitive jars come from starters. **No unused direct dependency was removed.** Spring Boot stays **3.2.3**, POI **5.2.3**.

## Runtime

| Name | Purpose |
| --- | --- |
| `spring-boot-starter` | CLI + logging |
| `spring-boot-starter-web` | API servlet (`api` profile) |
| `spring-boot-starter-validation` | Request DTOs |
| `spring-boot-starter-security` | JWT filter chain (web only) |
| `spring-boot-starter-data-jpa` | API persistence |
| `spring-boot-starter-mail` | `JavaMailSender` |
| `spring-boot-starter-actuator` | health/info/metrics (mail health off) |
| `flyway-core` | Migrations |
| `postgresql` | API database |
| `jjwt-*` | JWT |
| `poi-ooxml` 5.2.3 | `.xlsx` |
| `commons-csv` 1.10.0 | API CSV importer |
| `springdoc-openapi-starter-webmvc-ui` 2.3.0 | OpenAPI UI |

## Test

| Name | Purpose |
| --- | --- |
| `spring-boot-starter-test` | JUnit 5, Mockito, MockMvc |
| `h2` | Flyway-compatible API tests |

## Plugins

`spring-boot-maven-plugin`, `jacoco-maven-plugin` (excel/template/campaign), `spotless-maven-plugin` (optional format; not bound to verify).
