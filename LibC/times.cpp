#include <Kernel/Syscall.h>
#include <errno.h>
#include <sys/times.h>

clock_t times(struct tms* buf)
{
    int rc = syscall(SC_times, buf);
    __RETURN_WITH_ERRNO(rc, rc, (clock_t)-1);
}
