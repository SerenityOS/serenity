## Name

purge - release memory

## Synopsis

```**sh
$ purge [options]
```

## Description

This program instructs the kernel to release memory that can
be released without interacting with its userspace owner(s).

## Options

-   `-c`: Release all clean inode-backed memory.
-   `-v`: Release all purgeable memory currently marked volatile.

If no options are specified, all possible memory is released.

## Examples

```sh
# purge
Purged page count: 744
```
