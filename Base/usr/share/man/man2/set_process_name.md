## Name

set\_process\_name - change the process name

## Synopsis

```**c++
#include <unistd.h>

int set_process_name(const char* name, size_t name_length);
```

## Description

`set_process_name()` changes the name of the calling process to the string `name` with length `name_length`. 

## Pledge

In pledged programs, the `proc` promise is required for this system call.

## Errors

* `EFAULT`: `name` is not in readable memory.
* `ENAMETOOLONG`: `name_length` is too long.

## See also

* [`get_process_name`(2)](../man2/get_process_name.md)
