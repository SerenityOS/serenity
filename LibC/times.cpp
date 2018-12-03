#include <sys/times.h>
#include <errno.h>
#include <Kernel/Syscall.h>

clock_t times(struct tms* buf)
{
    int rc = Syscall::invoke(Syscall::SC_times, (dword)buf);
    __RETURN_WITH_ERRNO(rc, rc, (clock_t)-1);
}
