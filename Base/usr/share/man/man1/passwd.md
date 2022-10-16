## Name

passwd - Change account passwords

## Synopsis

```sh
$ passwd [-d|-l|-u] [username]
```

## Description

`passwd` modifies `/etc/shadow` to change, delete, lock, or unlock a user's password.
If the user executing the command is not root, `passwd` will prompt for the target user's password.

## Options

* `-d`, `--delete`: Delete password
* `-l`, `--lock`: Lock password
* `-u`, `--unlock`: Unlock password
If neither `-d`, `-l` or `-u` are specified, `passwd` will prompt to change the account password.

## Arguments

* `username`: Username of the account on which actions will be applied

## Files

* /etc/passwd
* /etc/shadow

## See also

* [`shadow`(4)](help://man/4/shadow)
* [`passwd`(4)](help://man/4/passwd)
