## Name

access - check if a file is accessible

## Synopsis

```**c++
#include <unistd.h>

int access(const char* path, int mode);
```

## Description

Check if a file at the given _path_ exists and is accessible to the current user for the given _mode_.
Valid flags for _mode_ are:

-   `F_OK` to check if the file is accessible at all,
-   `R_OK` to check if the file can be read,
-   `W_OK` to check if the file can be written to,
-   `X_OK` to check if the file can be executed as a program.

## Return value

If the file is indeed accessible for the specified _mode_, `access()` returns 0. Otherwise, it returns -1 and sets `errno` to describe the error.
