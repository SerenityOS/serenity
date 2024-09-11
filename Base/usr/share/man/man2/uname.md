## Name

uname - retrieve information about the current kernel

## Synopsis

```**c++
#include <sys/utsname.h>

int uname(struct utsname* buf);
```

## Description

Retrieves information about the current kernel and writes it into the `utsname`
structure pointed to by `buf`.

```**c++
struct utsname {
    char sysname[];
    char nodename[];
    char release[];
    char version[];
    char machine[];
};
```

## Return value

If successful, returns 0. Otherwise, returns -1 and sets `errno` to describe the error.

## Errors

-   `EFAULT`: `buf` is not a writable address.

## See also

-   [`uname`(1)](help://man/1/uname)
