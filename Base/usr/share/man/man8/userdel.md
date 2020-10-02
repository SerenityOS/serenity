## Name

userdel - delete a user account

## Synopsis

```**sh
# userdel [-r] <login>
```

## Description

This program deletes a user account in the system.

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
* `/home/` - user home directory is deleted if the `-r` flag is specified.

## Examples

```sh
# userdel alice
# userdel -r alice
# userdel --remove alice
```

## See Also

* [`useradd`(8)](useradd.md)
