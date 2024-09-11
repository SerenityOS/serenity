## Name

lsdev - list devices' major number allocations

## Synopsis

```**sh
$ lsdev
```

## Description

lsdev is a utility for displaying devices' major number allocations, for
character and block devices both. It shows the allocated number and its
family name per allocation.

## Files

-   `/sys/kernel/chardev_major_allocs` - list of the major number allocations for character devices.
-   `/sys/kernel/blockdev_major_allocs` - list of the major number allocations for block devices.

## Examples

```sh
$ lsdev
```
