## Name

get_process_name - get the process name

## Synopsis

```**c++
#include <unistd.h>

int get_process_name(char* buffer, int buffer_length);
```

## Description

`get_process_name()` places the current process name into the provided `buffer`.

## Pledge

In pledged programs, the `stdio` promise is required for this system call.

## Errors

-   `EFAULT`: the process name could not be copied into the buffer.
-   `ENAMETOOLONG`: `buffer_length` is too short.

## See also

-   [`set_process_name`(2)](help://man/2/set_process_name)
