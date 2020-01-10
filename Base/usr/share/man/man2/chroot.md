## Name

chroot - change filesystem root

## Synopsis

```**c++
#include <unistd.h>

int chroot(const char* path);
```

## Description

`chroot()` changes the filesystem root of the current process to a new directory specified by `path`.

## Errors

* `EPERM`: The current process does not have superuser privileges.
* `EFAULT`: `path` is not in readable memory.

All of the usual path resolution errors may also occur.

## See also

* [`chroot`(8)](../man8/chroot.md)
