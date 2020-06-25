## Name

pledge - reduce process capabilities

## Synopsis

```**c++
#include <unistd.h>

int pledge(const char* promises, const char* execpromises);
```

## Description

`pledge()` makes a promise to the kernel that from this moment on, the calling process will only use a subset of system functionality.

Functionality is divided into a curated set of promises (described below), which can be combined to cover the program's needs. Both arguments are space-separated lists of promises.

Note that `pledge()` can be called repeatedly to remove previously-pledged promises, but it can never regain capabilities once lost.

`promises` are applied to the current process, and will also be inherited by children created by [`fork`(2)](fork.md).

`execpromises` are applied if/when a new process image is created with [`exec(2)`](exec.md).

If `promises` or `execpromises` is null, the corresponding value is unchanged.

If the process later attempts to use any system functionality it has previously promised *not* to use, the process is instantly terminated. Note that a process that has not ever called `pledge()` is considered to not have made any promises, and is allowed use any system functionality (subject to regular permission checks).

`pledge()` is intended to be used in programs that want to sandbox themselves, either to limit the impact of a possible vulnerability exploitation, or before intentionally executing untrusted code.

## Promises

* `stdio`: Basic I/O, memory allocation, information about self, various non-destructive syscalls
* `thread`: The POSIX threading API (\*)
* `id`: Ability to change UID/GID
* `tty`: TTY related functionality
* `proc`: Process and scheduling related functionality
* `exec`: The [`exec(2)`](exec.md) syscall
* `unix`: UNIX local domain sockets
* `inet`: IPv4 domain sockets
* `accept`: May use [`accept(2)`](accept.md) to accept incoming socket connections on already listening sockets. It also allows [`getsockopt(2)`](getsockopt.md) with `SOL_SOCKET` and `SO_PEERCRED` on local sockets (\*)
* `rpath`: "Read" filesystem access
* `wpath`: "Write" filesystem access
* `cpath`: "Create" filesystem access
* `dpath`: Creating new device files
* `chown`: Changing file owner/group
* `fattr`: Changing file attributes/permissions
* `shared_buffer`: Shared memory buffers (\*)
* `chroot`: The [`chroot(2)`](chroot.md) syscall (\*)
* `video`: May use [`ioctl(2)`](ioctl.md) and [`mmap(2)`](mmap.md) on framebuffer video devices
* `settime`: Changing the system time and date
* `setkeymap`: Changing the system keyboard layout (\*)
* `sigaction`: Change signal handlers and dispositions (\*)
* `sendfd`: Send file descriptors over a local socket
* `recvfd`: Receive file descriptors over a local socket

Promises marked with an asterisk (\*) are SerenityOS specific extensions not supported by the original OpenBSD `pledge()`.

## Errors

* `EFAULT`: `promises` and/or `execpromises` are not null and not in readable memory.
* `EINVAL`: One or more invalid promises were specified.
* `EPERM`: An attempt to increase capabilities was rejected.

## History

The `pledge()` system call was first introduced by OpenBSD. The implementation in SerenityOS differs in many ways and is by no means final.

## See also

* [`unveil`(2)](unveil.md)
