## Name

chroot - run a shell with a different filesystem root

## Synopsis

```**sh
# chroot <path> [program] [-o options]
```

## Description

This program uses the [`chroot`(2)](../man2/chroot.md) syscall to switch into a
different filesystem root and spawn a shell inside it.

It runs the given *program* (by default, `/bin/Shell`) inside the new root.
Mount options can be given in the same format as for [`mount`(8)](mount.md).

## Examples

```sh
# chroot /var/chroot
# pwd
/
```

## See also

* [`chroot`(2)](../man2/chroot.md)
* [`mount`(8)](mount.md)
