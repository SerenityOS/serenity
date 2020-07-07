## Name

setuid, setgid - set user / group ID

## Synopsis

```**c++
#include <unistd.h>

int setuid(uid_t);
int setgid(gid_t);
```

## Description

Sets all of real, effective, and saved user or group ID to the given ID.

For non-superusers, the given ID has to be equal to the current real or effective ID for the call to succeed.

## Return value

If the call was set successful, returns 0.
Otherwise, returns -1 and sets `errno` to describe the error.

## Errors

* `EPERM`: The new ID is not equal to the real ID or effective ID, and the user is not superuser.

## See also

* [`setuid_overview`(7)](../man7/setuid_overview.md)
* [`geteuid`(2) / `getegid`(2)](geteuid.md)
* [`getuid`(2) / `getgid`(2)](getuid.md)
* [`getresuid`(2) / `getresgid`(2)](getresuid.md)
* [`seteuid`(2) / `setegid`(2)](seteuid.md)
* [`setresuid`(2) / `setresgid`(2)](setresuid.md)
