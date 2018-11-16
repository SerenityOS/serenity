#include <errno.h>
#include <stdarg.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <Kernel/Syscall.h>

extern "C" {

int ioctl(int fd, unsigned request, ...)
{
    va_list ap;
    va_start(ap, request);
    unsigned arg = va_arg(ap, unsigned);
    int rc = Syscall::invoke(Syscall::SC_ioctl, (dword)fd, (dword)request, (dword)arg);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

}

