## Name

geteuid, getegid - get effective user / group id

## Synopsis

```**c++
#include <unistd.h>

uid_t geteuid(void);
gid_t getegid(void);
```

## Description

Returns the effective user or group id.

## See also

-   [`setuid_overview`(7)](help://man/7/setuid_overview)
-   [`getuid`(2) / `getgid`(2)](help://man/2/getuid)
-   [`getresuid`(2) / `getresgid`(2)](help://man/2/getresuid)
-   [`seteuid`(2) / `setegid`(2)](help://man/2/seteuid)
-   [`setuid`(2) / `setgid`(2)](help://man/2/setuid)
-   [`setresuid`(2) / `setresgid`(2)](help://man/2/setresuid)
