## Name

Application files (.af)

## Description

Application files are a subset of INI-format.
They contain application information:

- name
- executable path
- category
- description (optional)

and launcher information (optional):

- supported file types
- protocols

Additionally, application files define System Menu entries.

------------------------------------------
All application files are stored in **read-only** memory in `/res/apps`.

## Examples

*/res/apps/Calendar.af*

```ini
[App]
Name=Calendar
Executable=/bin/Calendar
Category=Utilities
```

## See also

- [INI (5)](help://man/5/INI)