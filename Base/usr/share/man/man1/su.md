## Name

su - switch to another user

## Synopsis

```sh
$ su [-] [user]
$ su [-] [user] [-c command]
```

## Description

`su`: Switch to another user.

When called with no user-specified, `su` defaults to switch to the _root_ user. Need to enter the password if the user switch to has one.

## Options

-   `-`, `-l`, `--login`: Start the shell as it was a real login
-   `-c`, `--command`: Execute a command using `/bin/sh` instead of starting an interactive shell

## Arguments

-   `user`: User to switch to (defaults to the user with UID 0)

## Examples

Switch to root user

```sh
$ su
```

Switch to another user

```sh
$ su nona
```

## See also

-   [`pls`(8)](help://man/8/pls)
