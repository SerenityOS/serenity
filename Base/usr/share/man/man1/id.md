## Name

id - print user/group ID

## Synopsis

```**sh
$ id [options...]
```

## Description

`id` outputs user and group information for the current user.

## Options

-   `-u`: Print only real UID.
-   `-g`: Print only real GID.
-   `-G`: Print only all GIDs.
-   `-n`: If `-u`, `-g` or `-G` is specified, print full names instead of IDs.

## Examples

```sh
$ id
uid=100(anon) gid=100(users) groups=1(wheel),10(lookup),12(notify),4(audio),13(window),14(clipboard),3(phys)
$ id -u
100
$ id -g
100
$ id -un
anon
$ id -G
1 10 12 4 13 14 3
```

## See also

-   [`whoami`(1)](help://man/1/whoami)
-   [`groups`(1)](help://man/1/groups)
-   [`usermod`(8)](help://man/8/usermod)
