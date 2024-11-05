## Name

groupdel - delete a group

## Synopsis

```**sh
# groupdel <group>
```

## Description

This program deletes a group in the system.

This program must be run as root.

## Caveats

You may not remove the primary group of any existing user. You must remove the user before you remove the group.

You should manually check all file systems to ensure that no files remain owned by this group.

You should manually check all users to ensure that no user remain in this group.

## Exit Values

-   0 - Success
-   1 - Couldn't update the group file
-   6 - Specified group doesn't exist
-   8 - Can't remove user's primary group

## Files

-   `/etc/group` - group information (such as GID) in this file is deleted.

## Examples

```sh
# groupdel alice
```

## See Also

-   [`userdel`(8)](help://man/8/userdel)
-   [`groupadd`(8)](help://man/8/groupadd)
-   [`groups`(1)](help://man/1/groups)
