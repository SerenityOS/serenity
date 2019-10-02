## Name

create\_shared\_buffer - create a shareable memory buffer

## Synopsis
```**c++
#include <SharedBuffer.h>

int create_shared_buffer(int size, void** buffer);
```

## Description

Creates a new memory region that can be shared with other processes. The region is only accessible to the creating process by default.

## Return value

If a region is successfully created, `create_shared_buffer()` stores a pointer to the memory in `buffer` and returns a buffer ID. Otherwise, it returns -1 and sets `errno` to describe the error.

## Errors

* `EINVAL`: `size` is zero or negative.
* `EFAULT`: `buffer` is not a valid address.

## See also

* [`share_buffer_with`(2)](share_buffer_with.md)
