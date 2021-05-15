## Name

getppid - get current process's parent process ID

## Synopsis

```**c++
#include <unistd.h>

pid_t getppid();
```

## Description

Returns the PID (process ID) of the parent of the calling process.

## Return value

The process ID of the parent of the calling process.

## Errors

None.

## See also

* [`getpid`(2)](getpid.md)
* [`gettid`(2)](gettid.md)
