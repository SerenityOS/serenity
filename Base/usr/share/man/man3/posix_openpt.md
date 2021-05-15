## Name

posix\_openpt - open a pseudo-terminal device

## Synopsis

```**c++
#include <stdlib.h>
#include <fcntl.h>

int posix_openpt(int flags);
```

## Description

Open a pseudo-terminal master using the given *flags*.

The *flags* argument accepts a bitmask of the following flags:

* `O_RDWR`: Open for both reading and writing.
* `O_NOCTTY`: The opened pseudo-terminal will not be made the controlling TTY for the process.
* `O_CLOEXEC`: The opened fd shall be closed on [`exec`(2)](../man2/exec.md).

## Return value

On success, a pseudo-terminal device is allocated and `posix_openpt()` returns a file descriptor for it. Otherwise, it returns -1 and sets `errno` to describe the error.

## Errors

Returns the same errors as [`open`(2)](../man2/open.md).

## See also

* [`open`(2)](../man2/open.md)
