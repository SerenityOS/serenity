#include "time.h"
#include "errno.h"
#include <Kernel/Syscall.h>

extern "C" {

time_t time(time_t* tloc)
{
    struct timeval tv;
    struct timezone tz;
    if (gettimeofday(&tv, &tz) < 0)
        return (time_t)-1;
    if (tloc)
        *tloc = tv.tv_sec;
    return tv.tv_sec;
}

int gettimeofday(struct timeval* tv, struct timezone*)
{
    int rc = Syscall::invoke(Syscall::SC_gettimeofday, (dword)tv);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

}
