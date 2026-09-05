# Excel format

Recipient lists for SendEmailService. Path: `mail.excel-file-path` / `MAIL_EXCEL_FILE_PATH`. The first sheet is the only sheet read.

## Supported file types

| Type | Supported |
| --- | --- |
| `.xlsx` (Office Open XML) | Yes |
| `.xls` (legacy Excel) | No |
| CSV / TSV / Google Sheets export as CSV | No |

A missing file, a directory, or a non-`.xlsx` path fails before any mail is sent (`ExcelProcessingException`). Cells are read with Apache POI `DataFormatter` (formulas and numbers become trimmed text).

## Required columns

With a **header row** (recommended), the first row must include both:

- **Email** — aliases: `email`, `e-mail` / `e_mail`, `mail` (case-insensitive; punctuation stripped)
- **Name** — aliases: `name`, `full name` / `fullname` / `full_name`

Column order does not matter when headers are present.

If the first row has an Email header but no Name header (and column A is not an address containing `@`), the job fails: *Unable to process the Excel file because the Name column was not found.* The reverse (Name without Email) fails the same way for the Email column.

**Headerless sheet:** if the first row is not recognized as both headers, **column A is email** and **column B is name**. Extra columns are not mapped in this mode.

## Optional columns

Any other header becomes a placeholder key: letters, digits, and `_` are kept; the rest is dropped; the key is lowercased. Example: `Company` → `company` → `{{company}}`.

Optional columns are ignored in headerless mode.

## Supported placeholders

Always available after a valid row:

- `{{name}}` / `{{Name}}` (same key)
- `{{email}}` / `{{Email}}`

Plus one key per extra header. The body template must only use keys that exist in the imported data, or template validation fails before SMTP.

## Invalid rows

These rows are **skipped** (counted as invalid). The rest of the file still loads:

- Empty / missing Excel row
- Entirely blank email, name, and extra cells
- Blank email (name present)
- Email with no `@`

The log line is `Skipping row N: …`. Counts: `Total rows`, `Valid contacts`, `Invalid contacts`, `Duplicates`.

## Duplicate handling

Duplicate **email** values in the same file are case-insensitive (`Ada@Example.com` matches `ada@example.com`). The **first** valid occurrence is kept; later rows are counted as duplicates and skipped (not mailed).

A later **process** also skips addresses already listed in `mail.sent-log-path` (after a real send). Dry-run does not append that log.

## Example

```text
Name        Email              Company
Rahul       rahul@example.com  ABC
Priya       priya@example.com  XYZ
```

Equivalent headers: `Email`, `Name`, `Company` in any column order. Template example:

```text
Hi {{Name}},

This was sent to {{Email}} at {{Company}}.
```
