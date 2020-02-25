## Name

man - read manual pages

## Synopsis

```**sh
$ man page
$ man section page
```

## Description

`man` finds, loads and displays the so-called manual pages,
or man pages for short, from the SerenityOS manual. You're reading
the manual page for `man` program itself right now.

## Sections

The SerenityOS manual is split into the following *sections*, or *chapters*:

1. Command-line programs
2. System calls
3. Libraries
4. Special files
5. File formats
6. Games
7. Miscellanea
8. Sysadmin tools

Sections are subject to change in the future.

## Examples

To open documentation for the `echo` command:
```sh
$ man echo
```

To open the documentation for the `mkdir` command:
```sh
$ man 1 mkdir
```
Conversely, to open the documentation about the `mkdir()` syscall:
```sh
$ man 2 mkdir
```

## Files

`man` looks for man pages under `/usr/share/man`. For example,
this man page should be located at `/usr/share/man/man1/man.md`.
