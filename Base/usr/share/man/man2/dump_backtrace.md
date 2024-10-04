## Name

dump_backtrace - dump the process's current kernel backtrace

## Synopsis

```**c++
#include <unistd.h>

void dump_backtrace();
```

## Description

`dump_backtrace` prints the process's kernel backtrace to the serial console. This is most useful for information about a process crash. Note that the userspace backtrace, which in general is more useful, is not printed by this syscall, since it can be printed entirely by userspace examining its own stack.

The output may look something like this:

```
254.838 [#0 crash(55:55)]: Kernel + 0x000000000052b9f4  Kernel::Process::crash(int, AK::Optional<Kernel::RegisterState const&>, bool) +0x394
254.838 [#0 crash(55:55)]: Kernel + 0x000000000050f1d4  Kernel::handle_crash(Kernel::RegisterState const&, char const*, int, bool) +0x2ec
254.838 [#0 crash(55:55)]: Kernel + 0x000000000059e648  illegal_instruction_asm_entry +0x30
```

## Errors

This syscall does not return any errors to the user.

## See Also

-   [`dbgputstr`(2)](help://man/2/dbgputstr)
-   [`crash`(2)](help://man/1/crash)
-   [`CrashReporter(1)`](help://man/1/Applications/CrashReporter)
