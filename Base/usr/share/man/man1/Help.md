## Name

![Icon](/res/icons/16x16/app-help.png) Help - digital manual

[Open](file:///bin/Help)

## Synopsis

```**sh
$ Help
$ Help [section] page
$ Help search_query
$ Help file
```

## Description

`Help` is Serenity's digital manual, the GUI counterpart to `man`.
It lets you search for and read manual pages (or "man pages").

## Sections

The SerenityOS manual is split into the following *sections*, or *chapters*:

1. User Programs
2. System Calls
3. Library Functions
4. Special Files
5. File Formats
6. Games
7. Miscellanea
8. Sysadmin Tools

Sections are subject to change in the future.

## Examples

To open Help:
```sh
$ Help
```

To open documentation for the `echo` command:
```sh
$ Help echo
```

To open the documentation for the `mkdir` command:
```sh
$ Help 1 mkdir
```
Conversely, to open the documentation about the `mkdir()` syscall:
```sh
$ Help 2 mkdir
```

## Files

`Help` looks for man pages under `/usr/share/man`. For example,
this man page should be located at `/usr/share/man/man1/Help.md`.

## See Also

* [`man`(1)](help://man/1/man) To read these same man pages from the terminal

