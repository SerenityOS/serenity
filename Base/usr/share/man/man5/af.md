## Name

af - Application File format (.af)

## Synopsis

The Application Files define System Menu entries and launcher file types / protocols.

## Description

Application files are a subset of the INI format.
They have no easily detectable filemagic and contain application information (rough overview):

- name
- executable path
- category
- description (optional)

and launcher information (optional):

- supported file types
- protocols

------------------------------------------
All application files are stored in **read-only** memory in `/res/apps`.

## Examples

[`/res/apps/Calendar.af`](../../../../res/apps/Calendar.af)

```ini
[App]
Name=Calendar
Executable=/bin/Calendar
Category=Utilities
```

## See also

- [ini(5)](help://man/5/ini)
- [`Userland/Services/Taskbar/main.cpp`](../../../../../Userland/Services/Taskbar/main.cpp)
- `Launcher::load_handlers`
  in [`Userland/Services/LaunchServer/Launcher.cpp`](../../../../../Userland/Services/LaunchServer/Launcher.cpp)
