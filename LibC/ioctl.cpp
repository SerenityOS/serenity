#include <Kernel/Syscall.h>
#include <errno.h>
#include <stdarg.h>
#include <stdio.h>
#include <sys/ioctl.h>

extern "C" {

int ioctl(int fd, unsigned request, ...)
{
    va_list ap;
    va_start(ap, request);
    unsigned arg = va_arg(ap, unsigned);
    int rc = syscall(SC_ioctl, fd, request, arg);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
