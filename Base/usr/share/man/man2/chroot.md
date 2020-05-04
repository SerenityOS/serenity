## Name

chroot, chroot\_with\_mount\_flags - change filesystem root

## Synopsis

```**c++
#include <unistd.h>

int chroot(const char* path);
int chroot_with_mount_flags(const char* path, int mount_flags);
```

## Description

`chroot()` changes the filesystem root of the current process to a new directory specified by `path`.

`chroot_with_mount_flags()` additionally applies the given `mount_flags` to the new root directory
of the current process as if it was a separate mount. All the same flags as for [`mount`(2)](mount.md)
are accepted, except `MS_BIND` doesn't make sense for chroots. Additionally, the value -1 is accepted,
and makes `chroot_with_mount_flags()` behave like regular `chroot()`.

## Errors

* `EPERM`: The current process does not have superuser privileges.
* `EFAULT`: `path` is not in readable memory.

All of the usual path resolution errors may also occur.

## See also

* [`chroot`(8)](../man8/chroot.md)
* [`mount`(2)](mount.md)
