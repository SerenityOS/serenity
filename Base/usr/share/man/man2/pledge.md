## Name

pledge - reduce process capabilities

## Synopsis

```**c++
#include <unistd.h>

int pledge(const char* promises, const char* execpromises);
```

## Description

`pledge()` makes a promise to the kernel that from this moment on, the calling process will only use a subset of system functionality.

Functionality is divided into a curated set of promises (described below), which can be combined to cover the program's needs. Both arguments are space-separated lists of promises, with each promise potentially prefixed with a '-'.

`pledge()` can be called repeatedly to forfeit previously-pledged promises, but it can never regain capabilities once lost. Previously-pledged promises can be forfeited by either specifying a subset of the previously pledged promises, or by prepending a '-' character to one or more promises in which case only these "negative" promises will be forfeited. It is an error to mix negative and non-negative promises in the same `pledge()` call. A negative promise has no effect if the same promise has already been forfeited.

`promises` are applied to the current process, and will also be inherited by children created by [`fork`(2)](help://man/2/fork).

`execpromises` are applied if/when a new process image is created with [`exec`(2)](help://man/2/exec).

If `promises` or `execpromises` is null, the corresponding value is unchanged.

If the process later attempts to use any system functionality it has previously promised *not* to use, the process is instantly terminated. Note that a process that has not ever called `pledge()` is considered to not have made any promises, and is allowed use any system functionality (subject to regular permission checks).

`pledge()` is intended to be used in programs that want to sandbox themselves, either to limit the impact of a possible vulnerability exploitation, or before intentionally executing untrusted code.

## Promises

* `stdio`: Basic I/O, memory allocation, information about self, various non-destructive syscalls
* `thread`: The POSIX threading API (\*)
* `id`: Ability to change UID/GID
* `tty`: TTY related functionality
* `proc`: Process and scheduling related functionality
* `exec`: The [`exec`(2)](help://man/2/exec) syscall
* `unix`: UNIX local domain sockets
* `inet`: IPv4 domain sockets
* `accept`: May use [`accept`(2)](help://man/2/accept) to accept incoming socket connections on already listening sockets (\*)
* `rpath`: "Read" filesystem access
* `wpath`: "Write" filesystem access
* `cpath`: "Create" filesystem access
* `dpath`: Creating new device files
* `chown`: Changing file owner/group
* `fattr`: Changing file attributes/permissions
* `video`: May use [`ioctl`(2)](help://man/2/ioctl) and [`mmap`(2)](help://man/2/mmap) on framebuffer video devices
* `settime`: Changing the system time and date
* `setkeymap`: Changing the system keyboard layout (\*)
* `sigaction`: Change signal handlers and dispositions (\*)
* `sendfd`: Send file descriptors over a local socket
* `recvfd`: Receive file descriptors over a local socket
* `ptrace`: The [`ptrace`(2)](help://man/2/ptrace) syscall (\*)
* `prot_exec`: [`mmap`(2)](help://man/2/mmap) and [`mprotect`(2)](help://man/2/mprotect) with `PROT_EXEC`
* `map_fixed`: [`mmap`(2)](help://man/2/mmap) with `MAP_FIXED` or `MAP_FIXED_NOREPLACE` (\*)
* `mount`: [`mount`(2)](help://man/2/mount) Various filesystem mount related syscalls (\*)
* `no_error`: Ignore requests of pledge elevation going forwards, this is useful for enforcing _execpromises_ while the child process wants to ask for more upfront (Note that the elevation requests are _not_ granted, merely ignored), this is similar to the `error` pledge in OpenBSD.
* `jail`: Various jail-specific syscalls (\*)

Promises marked with an asterisk (\*) are SerenityOS specific extensions not supported by the original OpenBSD `pledge()`.

## Errors

* `EFAULT`: `promises` and/or `execpromises` are not null and not in readable memory.
* `EINVAL`: One or more invalid promises were specified.
* `EINVAL`: Negative promise(s) `pledge()`d before any non-negative `pledge()` call.
* `EINVAL`: Negative promises were mixed with non-negative promises in the same `pledge()` call.
* `EPERM`: An attempt to increase capabilities was rejected.
* `E2BIG`: `promises` string or `execpromises `string are longer than all known promises strings together.

## Examples

Example using negative pledges:

```**c++
// Initial pledge
pledge("stdio rpath wpath id", NULL);

// Stuff that needs "id" here...

// Forfeit id
pledge("-id", NULL);

// Stuff that needs rpath/wpath here...

// Forfeit rpath and wpath
pledge("-rpath -wpath", NULL);

// Only stdio remains pledged at this point.
```

## History

The `pledge()` system call was first introduced by OpenBSD. The implementation in SerenityOS differs in many ways and is by no means final.

## See also

* [`unveil`(2)](help://man/2/unveil)
* [`Mitigations`(7)](help://man/7/Mitigations)
