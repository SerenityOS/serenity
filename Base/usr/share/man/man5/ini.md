## Name

INI - generic config file format (.ini)

## Description

INI files serve as human-readable configuration files.

They consist of key-value pairs separated by '=', optionally located under a unique group in square brackets.

Additionally, [`Userland/Libraries/LibCore/ConfigFile.cpp`](../../../../../Userland/Libraries/LibCore/ConfigFile.cpp)
supports comments: the characters '#' and ';' skip the entire line only if they appear at the beginning of the line.

## Examples

[`/etc/Keyboard.ini`](../../../../etc/Keyboard.ini)

```ini
[Mapping]
Keymaps=en-us
```
