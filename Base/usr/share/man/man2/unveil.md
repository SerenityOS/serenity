## Name

unveil - restrict filesystem access

## Synopsis

```**c++
#include <unistd.h>

int unveil(const char* path, const char* permissions);
```

## Description

`unveil()` manipulates the process veil. The veil is a allowlist of paths on
the file system the calling process is allowed to access.

A process that has not made any `unveil()` calls is allowed to access the whole
filesystem (subject to the regular permission checks). A process that has made
one or more `unveil()` calls cannot access any paths except those that were
explicitly unveiled.

Calling `unveil()` allows the process to access the given `path`, which must be
an absolute path, according to the given `permissions` string, which may
include the following characters:

-   `r`: May read a file at this path
-   `w`: May write to a file at this path
-   `x`: May execute a program image at this path
-   `c`: May create or remove a file at this path
-   `b`: May browse directories at this path

A single `unveil()` call may specify multiple permission characters at once.
Subsequent `unveil()` calls may take away permissions from the ones allowed
earlier for the same file or directory. Note that it remains possible to unveil
subdirectories with any permissions.

Note that unveiling a path with any set of permissions does not turn off the
regular permission checks: access to a file which the process has unveiled for
itself, but has otherwise no appropriate permissions for, will still be rejected.
Unveiling a directory allows the process to access any files inside the
directory.

Calling `unveil()` with both `path` and `permissions` set to null locks the
veil; no further `unveil()` calls are allowed after that. Although `unveil()`
calls start to take effect the moment they are made, until the veil is locked,
it remains possible to sometimes circumvent the restrictions set by unveiling
files and directories contained inside a restricted directory with different
permissions.

When a process calls `fork()`, the unveil state is copied to the new process.
The veil state is reset after the program successfully performs an `execve()`
call.

`unveil()` is intended to be used in programs that want to sandbox themselves,
either to limit the impact of a possible vulnerability exploitation, or before
intentionally executing untrusted code.

## Return value

If successful, returns 0. Otherwise, returns -1 and sets `errno` to describe
the error.

## Errors

-   `EFAULT`: `path` and/or `permissions` are not null and not in readable
    memory.
-   `EPERM`: The veil is locked, or an attempt to add more permissions for an
    already unveiled path was rejected.
-   `EINVAL`: `path` is not an absolute path, or `permissions` are malformed.
-   `E2BIG`: `permissions` string is longer than 5 characters.

All of the usual path resolution errors may also occur.

## History

The `unveil()` system call was first introduced by OpenBSD.

## Examples

```c++
// Allow the process to read from /res:
unveil("/res", "r");

// Allow the process to read, write, and create the config file:
unveil("/etc/WindowServer.ini", "rwc");

// Allow the process to execute Calendar:
unveil("/bin/Calendar", "x");

// Allow the process to browse files from /usr/share:
unveil("/usr/share", "b");

// Disallow any further veil manipulation:
unveil(nullptr, nullptr);
```

## See also

-   [`unveil`(1)](help://man/1/unveil)
-   [`pledge`(2)](help://man/2/pledge)
-   [`Mitigations`(7)](help://man/7/Mitigations)
