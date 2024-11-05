## Name

pls - Execute a command as root

## Synopsis

```**sh
$ pls [command]
```

## Description

Executes a command as superuser (UID and GID 0). This command is only available for users in the `wheel` group.

It is possible to execute commands that contain hyphenated options via the use of `--`, which signifies the
end of command options. For example:

```sh
$ pls -- ls -la
```

## Examples

```sh
$ pls whoami
Password:
root
$
```

```sh
$ pls sh
Password:
# whoami
root
#
```

## See also

-   [`su`(1)](help://man/1/su)
