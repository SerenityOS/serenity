## Name

sendfd - send a file descriptor to a local socket peer

## Synopsis

```**c++
#include <sys/socket.h>

int sendfd(int sockfd, int fd);
```

## Description

Send an open file descriptor to a local socket peer connected via `sockfd`. This is a non-blocking call that will fail if there are too many file descriptors already waiting to be received by the peer.

File descriptors are sent out-of-band and do not affect the regular data streams.

## Return value

If a file descriptor is successfully sent, `sendfd()` returns 0. Otherwise, -1 is returned and `errno` is set to indicate the error.

## Errors

-   `EBADF`: `sockfd` or `fd` is not an open file descriptor.
-   `ENOTSOCK`: `sockfd` does not refer to a socket.
-   `EAFNOSUPPORT`: `sockfd` does not refer to a local domain socket.
-   `ENOTCONN`: `sockfd` refers to a socket which is not connected.
-   `EINVAL`: `sockfd` does not refer to a connected or accepted socket.
-   `EBUSY`: There are too many file descriptors already waiting to be received by the peer.

## History

`sendfd()` was first introduced in Plan 9 from User Space.

## See also

-   [`recvfd`(2)](help://man/2/recvfd)
