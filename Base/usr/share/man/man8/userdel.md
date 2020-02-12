## Name

userdel - delete a user account in the system password file

## Synopsis

```**sh
# userdel [options] <login>
```

## Description

This program uses adds a new user to the system.

This program must be run as root.

## Options

* `-r`, `--remove`: Remove the home directory for this user if the directory exists.

## Exit Values

* 0 - Success
* 1 - Couldn't update the password file
* 6 - Specified user doesn't exist
* 12 - Couldn't remove home directory

## Files

* `/etc/passwd` - user information (such as UID and GID) in this file is deleted.
* `/home/` - user home directroy is deleted if the `-r` flag is specified.

## Examples

```sh
# userdel alice
# userdel -r alice
# userdel --remove alice
```

