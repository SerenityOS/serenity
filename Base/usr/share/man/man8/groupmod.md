## Name

groupmod - modify an existing group

## Synopsis

```**sh
# groupmod [options] <group>
```

## Description

This program modifies an existing group in the system.

This program must be run as root.

## Options

-   `-n`, `--new-name` _new-name_: Rename the group to _new-name_.

## Exit Values

-   0 - Success
-   6 - Specified group doesn't exist
-   9 - Group with the requested new name already exists

## Files

-   `/etc/group` - group information is updated in this file.

## Examples

```sh
# groupmod -n developers contributors
```

## See Also

-   [`usermod`(8)](help://man/8/usermod)
-   [`groupadd`(8)](help://man/8/groupadd)
-   [`groupdel`(8)](help://man/8/groupdel)
-   [`groups`(1)](help://man/1/groups)
