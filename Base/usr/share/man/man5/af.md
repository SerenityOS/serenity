## Name

af - Application File format (.af)

## Synopsis

The Application Files define System Menu entries and launcher file types / protocols.

## Description

Application files are a subset of the INI format.
They have no easily detectable filemagic and contain application information (App group):

| Key           | Description                      |
| ------------- | -------------------------------- |
| Name          | name                             |
| Executable    | executable path                  |
| Category      | category (optional)              |
| Description   | description (optional)           |
| IconPath      | application icon path (optional) |
| RunInTerminal | run in terminal flag (optional)  |

and launcher information (Launcher group, optional):

| Key       | Description                           |
| --------- | ------------------------------------- |
| FileTypes | supported file types separated by ',' |
| Protocols | protocols separated by ','            |

All application files are stored in [`/res/apps`](../../../../res/apps).

## Examples

[`/res/apps/Calendar.af`](../../../../res/apps/Calendar.af)

```ini
[App]
Name=Calendar
Executable=/bin/Calendar
Category=Utilities
```

## See also

-   [ini(5)](help://man/5/ini)
-   [`Userland/Services/Taskbar/main.cpp`](../../../../../Userland/Services/Taskbar/main.cpp)
-   `Launcher::load_handlers` in [`Userland/Services/LaunchServer/Launcher.cpp`](../../../../../Userland/Services/LaunchServer/Launcher.cpp)
