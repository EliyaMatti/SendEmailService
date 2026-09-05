# Direct Maven dependencies (M1-030)

Reviewed 2026-09-05 against `pom.xml`. Transitive jars (Logback, Jakarta Mail, POI xmlbeans, JUnit, Mockito, …) come from these starters; they are not listed as extra `<dependency>` entries. **No unused direct dependency was removed.** **No major version upgrades** (Spring Boot stays 3.2.3, POI stays 5.2.3).

## spring-boot-starter

| | |
| --- | --- |
| Name | `org.springframework.boot:spring-boot-starter` |
| Version | 3.2.3 (parent) |
| Purpose | CLI Spring context, logging, autoconfigure (`MailSenderApplication`, `BatchMailRunner`) |
| Used? | Yes |
| Required? | Yes |

## spring-boot-starter-mail

| | |
| --- | --- |
| Name | `org.springframework.boot:spring-boot-starter-mail` |
| Version | 3.2.3 (parent) |
| Purpose | `JavaMailSender`, `MimeMessageHelper`, `MailProperties` (`SmtpEmailSender`) |
| Used? | Yes |
| Required? | Yes |

## spring-boot-starter-test

| | |
| --- | --- |
| Name | `org.springframework.boot:spring-boot-starter-test` |
| Version | 3.2.3 (parent), `test` scope |
| Purpose | JUnit 5, Mockito, Spring Test (`@SpringBootTest`, `@MockBean`) |
| Used? | Yes |
| Required? | Yes (tests) |

## poi-ooxml

| | |
| --- | --- |
| Name | `org.apache.poi:poi-ooxml` |
| Version | 5.2.3 |
| Purpose | `.xlsx` read (`XSSFWorkbook` in `ExcelReader`) |
| Used? | Yes |
| Required? | Yes |

Not added: `spring-boot-starter-web` (no HTTP API). Not present: Maven Wrapper.

## Build plugins

| Name | Version | Purpose | Used? | Required? |
| --- | --- | --- | --- | --- |
| `spring-boot-maven-plugin` | 3.2.3 (parent) | `mvn spring-boot:run`, executable jar | Yes | Yes |
| `jacoco-maven-plugin` | 0.8.11 (pinned; Boot 3.2 line) | Coverage report for excel/template/campaign (M1-023) | Yes | Yes for that report |
| `spotless-maven-plugin` | 2.43.0 | Google Java Format | Optional locally | Not required to compile |

Spotless is not bound to `verify` by default; it is unused at runtime.
