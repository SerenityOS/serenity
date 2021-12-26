#include <Kernel/Syscall.h>
#include <errno.h>
#include <fcntl.h>
#include <stdarg.h>
#include <stdio.h>

extern "C" {

int fcntl(int fd, int cmd, ...)
{
    va_list ap;
    va_start(ap, cmd);
    u32 extra_arg = va_arg(ap, u32);
    int rc = syscall(SC_fcntl, fd, cmd, extra_arg);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
