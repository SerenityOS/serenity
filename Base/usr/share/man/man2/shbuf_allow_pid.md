## Name

shbuf\_allow\_pid - allow another process to map a shareable buffer

## Synopsis
```**c++
#include <SharedBuffer.h>

int shbuf_allow_pid(int shbuf_id, pid_t peer_pid);
```

## Description

Gives the process with PID `peer_pid` permission to map the shareable buffer with ID `shbuf_id`.

## Return value

On success, returns 0. Otherwise, returns -1 and `errno` is set.

## Errors

* `EINVAL`: `peer_pid` is invalid, or `shbuf_id` is not a valid ID.
* `EPERM`: The calling process does not have access to the buffer with `shbuf_id`.
* `ESRCH`: No process with PID `peer_pid` is found.

## See also

* [`shbuf_create`(2)](shbuf_create.md)
