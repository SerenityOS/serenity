## Name

accept - accept a new connection on a server socket

## Synopsis

```**c++
#include <sys/socket.h>

int accept(int sockfd, sockaddr* addr, socklen_t* addrlen);
```

## Description

Accept a new connection from a client for the server specified by `sockfd`. This function will block until there is at least one client trying to connect to the server, with other clients being queued up for accepting.

When `accept(2)` is successful, a new socket with a unique file descriptor is created and that file descriptor returned. If not null, the `addr` argument will contain the address of the newly-connected client. If not null, the `addrlen` argument will contain the maximum length of the client address that should be written, and it will in turn be overwritten with the actual length of the client address written back to `addr`.

## Return value

If the return value is positive, it represents the file descriptor of the new socket connected to the client that was accepted. If the return value is -1, the error can be found in `errno`, where the most important errors are:

-   `EBADFD`: The file descriptor `sockfd` is invalid.
-   `ENOTSOCK`: The given file descriptor `sockfd` is valid, but does not point to a socket.
-   `EMFILE`: No more file descriptors are available for the new socket.
-   `EAGAIN`: The socket was specified to be non-blocking, and there is no client in the queue. The user should try to `accept(2)` again at a later point.
-   `EINTR`: A signal interrupted the blocking.
