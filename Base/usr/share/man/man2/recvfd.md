## Name

recvfd - receive a file descriptor from a local socket peer

## Synopsis

```**c++
#include <sys/socket.h>

int recvfd(int sockfd, int options);
```

## Description

Receive an open file descriptor from a local socket peer connected via `sockfd`. This is a non-blocking call that will fail if there is no file descriptor waiting in the socket's queue.

File descriptors are sent out-of-band and do not affect the regular data streams.

The _options_ argument accepts a bitmask of the following flags:

-   `O_CLOEXEC`: The opened fd shall be closed on [`exec`(2)](help://man/2/exec).

## Return value

If a file descriptor is successfully received, it is returned as a non-negative integer. Otherwise, -1 is returned and `errno` is set to indicate the error.

## Errors

-   `EBADF`: `sockfd` is not an open file descriptor.
-   `ENOTSOCK`: `sockfd` does not refer to a socket.
-   `EAFNOSUPPORT`: `sockfd` does not refer to a local domain socket.
-   `EINVAL`: `sockfd` does not refer to a connected or accepted socket.
-   `EAGAIN`: There is no file descriptor queued on this socket.

## History

`recvfd()` was first introduced in Plan 9 from User Space.

## See also

-   [`sendfd`(2)](help://man/2/sendfd)
