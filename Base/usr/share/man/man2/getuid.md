## Name

getuid, getgid - get real user / group id

## Synopsis

```**c++
#include <unistd.h>

uid_t getuid(void);
gid_t getgid(void);
```

## Description

Returns the real user or group id.

## See also

-   [`setuid_overview`(7)](help://man/7/setuid_overview)
-   [`geteuid`(2) / `getegid`(2)](help://man/2/geteuid)
-   [`getresuid`(2) / `getresgid`(2)](help://man/2/getresuid)
-   [`seteuid`(2) / `setegid`(2)](help://man/2/seteuid)
-   [`setuid`(2) / `setgid`(2)](help://man/2/setuid)
-   [`setresuid`(2) / `setresgid`(2)](help://man/2/setresuid)
