## Name

getresuid, getresgid - get real, effective, and saved user / group ID

## Synopsis

```**c++
#include <unistd.h>

int getresuid(uid_t*, uid_t*, uid_t*);
int getresgid(gid_t*, gid_t*, gid_t*);
```

## Description

Returns the real, effective, and saved user or group ID.

## Return value

If the call was set successful, returns 0.
Otherwise, returns -1 and sets `errno` to describe the error.

## Errors

-   `EFAULT`: One of the passed pointers is not valid, writeable pointer in the current process.

## See also

-   [`setuid_overview`(7)](help://man/7/setuid_overview)
-   [`geteuid`(2) / `getegid`(2)](help://man/2/geteuid)
-   [`getuid`(2) / `getgid`(2)](help://man/2/getuid)
-   [`seteuid`(2) / `setegid`(2)](help://man/2/seteuid)
-   [`setuid`(2) / `setgid`(2)](help://man/2/setuid)
-   [`setresuid`(2) / `setresgid`(2)](help://man/2/setresuid)
