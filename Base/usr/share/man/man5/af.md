## Name

af - Application File format

## Synopsis

The Application Files define System Menu entries and launcher file types / protocols.

## Description

.af files are human-readable and are a subset of the INI-format, have no easily detectable filemagic. These files define System Menu entries and launcher file types / protocols.

They are stored in [`/res/apps`](../../../../res/apps).

## See Also

- [`Userland/Services/Taskbar/main.cpp`](../../../../../Userland/Services/Taskbar/main.cpp)
- `Launcher::load_handlers` in [`Userland/Services/LaunchServer/Launcher.cpp`](../../../../../Userland/Services/LaunchServer/Launcher.cpp).
