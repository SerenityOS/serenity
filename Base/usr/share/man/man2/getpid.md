## Name

getpid - get current process ID

## Synopsis

```**c++
#include <unistd.h>

pid_t getpid();
```

## Description

Returns the PID (process ID) of the calling process.

## Return value

The process ID of the calling process.

## Errors

None.

## See also

* [`getppid`(2)](getppid.md)
* [`gettid`(2)](gettid.md)
