## Name

mkdir - create a directory

## Synopsis

```**c++
#include <sys/stat.h>

int mkdir(const char* path, mode_t mode);
```

## Description

Create a new empty directory at the given _path_ using the given _mode_.

## Return value

If the directory was created successfully, `mkdir()` returns 0. Otherwise,
it returns -1 and sets `errno` to describe the error.

## See also

-   [`mkdir`(1)](help://man/1/mkdir)
