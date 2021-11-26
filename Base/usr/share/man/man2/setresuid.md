## Name

setresuid, setresgid - set real, effective, and saved user / group ID

## Synopsis

```**c++
#include <unistd.h>

int setresuid(uid_t, uid_t, uid_t);
int setresgid(gid_t, gid_t, gid_t);
```

## Description

Sets all of real, effective, and saved user or group ID to the given values.

An argument of `-1` keeps the corresponding ID unchanged.

For non-superusers, each of the given values has to be equal to any of the current real, effective, or saved IDs for the call to succeed.

## Return value

If the call was set successful, returns 0.
Otherwise, returns -1 and sets `errno` to describe the error.

## Errors

* `EPERM`: At least one of the passed IDs was not equal to the current real, effective, or saved ID, and the user is not superuser.

## See also

* [`setuid_overview`(7)](../man7/setuid_overview.md)
* [`geteuid`(2) / `getegid`(2)](geteuid.md)
* [`getuid`(2) / `getgid`(2)](getuid.md)
* [`getresuid`(2) / `getresgid`(2)](getresuid.md)
* [`seteuid`(2) / `setegid`(2)](seteuid.md)
* [`setuid`(2) / `setgid`(2)](setuid.md)
