## Name

shbuf\_create - create a shareable memory buffer

## Synopsis
```**c++
#include <SharedBuffer.h>

int shbuf_create(int size, void** buffer);
```

## Description

Creates a new memory region that can be shared with other processes. The region is only accessible to the calling process by default.

## Return value

If a region is successfully created, `shbuf_create()` stores a pointer to the memory in `buffer` and returns a buffer ID. Otherwise, it returns -1 and sets `errno` to describe the error.

## Errors

* `EINVAL`: `size` is zero or negative.
* `EFAULT`: `buffer` is not a valid address.

## See also

* [`shbuf_allow_pid`(2)](shbuf_allow_pid.md)
