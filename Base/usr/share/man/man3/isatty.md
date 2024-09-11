## Name

isatty - check if a file descriptor is a TTY

## Synopsis

```**c++
#include <unistd.h>

int isatty(int fd);
```

## Description

Checks if the device inside a given file descriptor is a TTY device.

## Return value

If `fd` refers to a TTY device, returns 1. Otherwise, returns 0 and `errno` is set to describe the error.

## Errors

-   `EBADF`: `fd` is not an open file descriptor.
-   `ENOTTY`: `fd` refers to something that's not a TTY device.
-   `EINVAL`: `fd` refers to something that supports `ioctl()`, but is still not a TTY device.
