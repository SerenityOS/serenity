## Name

INI - generic config file format (.ini)

## Description

INI files serve as human-readable configuration files.

INI files consist key-value configuration data pairs separated by a unique group in square brackets.

Additionally, [`Userland/Libraries/LibCore/ConfigFile.cpp`](../../../../../Userland/Libraries/LibCore/ConfigFile.cpp)
supports comments: '#' and ';' will skip entire line (as '//' in C++).

## Examples

[`/etc/Keyboard.ini`](../../../../etc/Keyboard.ini)

```ini
[Mapping]
Keymaps=en-us
<<<<<<< HEAD
```
