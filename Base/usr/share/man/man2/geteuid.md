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

* [`setuid_overview`(7)](../man7/setuid_overview.md)
* [`getuid`(2) / `getgid`(2)](getuid.md)
* [`getresuid`(2) / `getresgid`(2)](getresuid.md)
* [`seteuid`(2) / `setegid`(2)](seteuid.md)
* [`setuid`(2) / `setgid`(2)](setuid.md)
* [`setresuid`(2) / `setresgid`(2)](setresuid.md)
