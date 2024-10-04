## Name

dbgputstr - print logs to the serial console

## Synopsis

```**c++
#include <stdio.h>

void dbgputstr(char const* characters, size_t length);
```

## Description

`dbgputstr` is Serenity's generic kernel-supported logging facility. Currently, logging submitted to `dbgputstr` is directly printed to the serial console.

`dbgputstr` takes as arguments a pointer to a string to be written, and the length of that string.

Users should access logging functionality via the `dbg`/`dbgln` functions, which add additional process information to the log output.

## Errors

The C library function does not propagate errors.

The system call itself can fail with the following errors:

-   `EFAULT`: Invalid or inaccessible string.

No error is reported if the output cannot be written to a serial device.

If the return value is positive, this indicates the actual number of characters written, in case the provided string was null-terminated before the length was reached.

## See Also

-   [`dump_backtrace`(2)](help://man/2/dump_backtrace)
