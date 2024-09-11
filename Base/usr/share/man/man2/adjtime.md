## Name

adjtime - gradually adjust system clock

## Synopsis

```**c++
#include <sys/time.h>

int adjtime(const struct timeval* delta, struct timeval* old_delta);
```

## Description

`adjtime()` gradually increments the system time by `delta`, if it is non-null.

Serenity OS slows down or speeds up the system clock by at most 1%, so adjusting the time by N seconds takes 100 \* n seconds to complete.

Calling `settimeofday()` or `clock_settime()` cancels in-progress time adjustments done by `adjtime`.

If `delta` is not null, `adjtime` can only called by the superuser.

If `old_delta` is not null, it returns the currently remaining time adjustment. Querying the remaining time adjustment does not need special permissions.

## Pledge

In pledged programs, the `settime` promise is required when `delta` is not null.

## Errors

-   `EFAULT`: `delta` and/or `old_delta` are not null and not in readable memory.
-   `EINVAL`: `delta` is not null and has a `tv_nsec` field that's less than 0 or larger or equal to `10^6`. Negative deltas should have a negative `tv_sec` field but a `tv_nsec` that's larger or equal zero. For example, a delta of -0.5 s is represented by `{-1, 500'000}`.
-   `EPERM`: `delta` is not null but geteuid() is not 0.
