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
this man page should be located at `/usr/share/man/man1/Applications/Help.md`.

## See Also

-   [`man`(1)](help://man/1/man) To read these same man pages from the terminal
-   [`man`(7)](help://man/7/man) For an overview on how manpages are organized
