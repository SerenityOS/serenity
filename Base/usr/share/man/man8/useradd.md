## Name

useradd - add a new user to the system password file

## Synopsis

```**sh
# useradd [options] <login>
```

## Description

This program adds a new user to the system.

By default, the user will be added to the **users** group (which has a GID of 100).

This program must be run as root.

## Options

-   `-u`, `--uid` _uid_: The user identifier for the new user. If not specified, an unused UID above `1000` will be auto-generated.
-   `-g`, `--gid` _group_: The group name or identifier for the new user. If not specified, it will default to 100 (the **users** group).
-   `-p`, `--password` _password_: The encrypted password for the new user. If not specified, it will default to blank.
-   `-s`, `--shell` _path-to-shell_: The shell binary for this login. The default is `/bin/Shell`.
-   `-m`, `--create-home`: Create the specified home directory for this new user.
-   `-d`, `--home-dir` _path_: Set the home directory for this user to path. By default, this is `/home/username`, where `username` is the value of login.
-   `-n`, `--gecos` _general-info_: GECOS information about this login. See [Wikipedia](https://en.wikipedia.org/wiki/Gecos_field) for more information.

## Exit Values

-   0 - Success
-   1 - Couldn't update the password file
-   3 - Invalid argument to option
-   4 - UID already in use
-   12 - Couldn't create home directory

## Files

-   `/etc/passwd` - new user information (such as UID and GID) is appended to this file.
-   `/home/` - user home directory is created here if the `-m` flag is specified.

## Examples

```sh
# useradd -u 300 -m kling
# useradd -m -u 400 --gid 200 --gecos "Sergey Bugaev" bugaevc
# useradd quaker
# useradd --gid 1000 -d /tmp/somedir -n "Dan MacDonald" danboid
# useradd --create-home supercomputer7
```

## See also

-   [`userdel`(8)](help://man/8/userdel)
-   [`usermod`(8)](help://man/8/usermod)
