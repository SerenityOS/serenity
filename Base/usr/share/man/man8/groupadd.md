## Name

groupadd - add a new group to the system group file

## Synopsis

```**sh
# groupadd [options] <group>
```

## Description

This program adds a new group to the system.

This program must be run as root.

## Options

-   `-g`, `--gid` _gid_: The group identifier for the new group. If not specified, an unused GID above `100` will be auto-generated.
-   `-U`, `--users` user-list: A comma-separated list of usernames to add as members of the new group

## Files

-   `/etc/group` - new group information (such GID) is appended to this file.

## Examples

```sh
# groupadd -g 110 contributors
# groupadd maintainers
```

## See Also

-   [`useradd`(8)](help://man/8/useradd)
-   [`groupdel`(8)](help://man/8/groupdel)
-   [`groups`(1)](help://man/1/groups)
