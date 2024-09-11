## Name

bindmount - create a bindmount from `source_fd` to a target path.

## Synopsis

```**c++
#include <LibCore/System.h>

ErrorOr<void> bindmount(int source_fd, StringView target, int flags);
```

## Description

`bindmount()` create a bindmount from `source_fd` to a target path `target`, with mount flags of `flags`.

The following `flags` are supported:

-   `MS_NODEV`: Disallow opening any devices from this file system.
-   `MS_NOEXEC`: Disallow executing any executables from this file system.
-   `MS_NOSUID`: Ignore set-user-id bits on executables from this file system.
-   `MS_RDONLY`: Mount the filesystem read-only.
-   `MS_WXALLOWED`: Allow W^X protection circumvention for executables on this file system.
-   `MS_AXALLOWED`: Allow anonymous executable mappings for executables on this file system.
-   `MS_NOREGULAR`: Disallow opening any regular files from this file system.

These flags can be used as a security measure to limit the possible abuses of the mounted file system.

## Errors

-   `EINVAL`: The `flags` value contains deprecated flags such as `MS_REMOUNT` or `MS_BIND`.
-   `EPERM`: The current process does not have superuser privileges.
-   `ENODEV`: The `source_fd` is not an open file descriptor to a valid filesystem inode.

All of the usual path resolution errors may also occur.

## See also

-   [`mount`(2)](help://man/2/mount)
