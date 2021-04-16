## Name

pls - Execute a command as root

## Synopsis

```**sh
$ pls [command]
```

## Description

Executes a command as the root user (uid and gid 0), given that the user executing `pls` is located in
the plsusers file.

It is possible to execute commands that contain hyphenated options via the use of `--`, which signifies the
end of command options. For example:

```sh
$ pls -- ls -la
```

## Files
/etc/plsusers - List of users that can run `pls`

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
