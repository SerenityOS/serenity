## Name

seteuid, setegid - set effective user / group ID

## Synopsis

```**c++
#include <unistd.h>

int seteuid(uid_t);
int setegid(gid_t);
```

## Description

Sets the effective user or group ID.

For non-superusers, the effective ID can only be set to the current real or saved ID.

In particular, `seteuid(geteuid())` will fail if the current effective user ID is not equal to the current real or saved ID.

## Return value

If the call was set successful, returns 0.
Otherwise, returns -1 and sets `errno` to describe the error.

## Errors

* `EPERM`: The new ID is not equal to the real ID or saved ID, and the user is not superuser.

## See also

* [`setuid_overview`(7)](../man7/setuid_overview.md)
* [`geteuid`(2) / `getegid`(2)](geteuid.md)
* [`getuid`(2) / `getgid`(2)](getuid.md)
* [`getresuid`(2) / `getresgid`(2)](getresuid.md)
* [`setuid`(2) / `setgid`(2)](setuid.md)
* [`setresuid`(2) / `setresgid`(2)](setresuid.md)
