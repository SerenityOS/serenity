## Name

share\_buffer\_with - allow another process to map a shareable buffer

## Synopsis
```**c++
#include <SharedBuffer.h>

int share_buffer_with(int shared_buffer_id, pid_t peer_pid);
```

## Description

Gives the process with PID `peer_pid` permission to map the shareable buffer with ID `shared_buffer_id`.

## Return value

On success, returns 0. Otherwise, returns -1 and `errno` is set.

## Errors

* `EINVAL`: `peer_pid` is invalid, or `shared_buffer_id` is not a valid ID.
* `EPERM`: The calling process does not have access to the buffer with `shared_buffer_id`.
* `ESRCH`: No process with PID `peer_pid` is found.

## See also

* [`create_shared_buffer`(2)](create_shared_buffer.md)
