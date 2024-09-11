## Name

gettid - get current thread ID

## Synopsis

```**c++
#include <unistd.h>

int gettid();
```

## Description

Returns the TID (thread ID) of the calling thread. The first thread in a process has the same TID and PID. Subsequently spawned threads will have unique thread ID's, but all share the same PID.

## Return value

The thread ID of the calling thread.

## Error

None.

## See also

-   [`getpid`(2)](help://man/2/getpid)
-   [`getppid`(2)](help://man/2/getppid)
