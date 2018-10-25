#include "time.h"
#include "errno.h"
#include <Kernel/Syscall.h>

extern "C" {

time_t time(time_t* tloc)
{
    timeval tv;
    if (gettimeofday(&tv) < 0)
        return (time_t)-1;
    return tv.tv_sec;
}

int gettimeofday(timeval* tv)
{
    int rc = Syscall::invoke(Syscall::PosixGettimeofday, (dword)tv);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

}
