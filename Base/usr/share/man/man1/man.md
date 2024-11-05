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

## Options

-   `-P pager`, `--pager pager`: Pager to pipe the man page to

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

## See Also

-   [`less`(1)](help://man/1/less) For the terminal pager that `man` uses by default
-   [`Help`(1)](help://man/1/Applications/Help) To read these same man pages in a GUI
-   [`man`(7)](help://man/7/man) For an overview on how manpages are organized
