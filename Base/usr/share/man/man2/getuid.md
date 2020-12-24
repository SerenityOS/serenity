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

* [`setuid_overview`(7)](../man7/setuid_overview.md)
* [`geteuid`(2) / `getegid`(2)](geteuid.md)
* [`getresuid`(2) / `getresgid`(2)](getresuid.md)
* [`seteuid`(2) / `setegid`(2)](seteuid.md)
* [`setuid`(2) / `setgid`(2)](setuid.md)
* [`setresuid`(2) / `setresgid`(2)](setresuid.md)
