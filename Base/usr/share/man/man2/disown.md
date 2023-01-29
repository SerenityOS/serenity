## Name

disown - disown a child process

## Synopsis

```**c++
#include <serenity.h>

int disown(pid_t pid);
```

## Description

`disown()` unparents a child process of the calling process. The child's parent PID is set to zero, which allows the kernel to automatically reap it upon death.

## Pledge

In pledged programs, the `proc` promise is required for this system call.

## Errors

* `ESRCH`: No process with PID `pid` was found.
* `ECHILD`: The target process is not a child of the calling process.

