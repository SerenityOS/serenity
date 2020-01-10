## Name

chroot - run a shell with a different filesystem root

## Synopsis

```**sh
# chroot
```

## Description

This program uses the [`chroot`(2)](../man2/chroot.md) syscall to switch into a
different filesystem root and spawn a shell inside it.

It will not work unless there is a `/bin/Shell` available inside the new root.

## Examples

```sh
# chroot /var/chroot
# pwd
/
```

## See also

* [`chroot`(2)](../man2/chroot.md)
