#include <time.h>
#include <errno.h>
#include <assert.h>
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
    int rc = syscall(SC_gettimeofday, tv);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

char* ctime(const time_t*)
{
    return const_cast<char*>("ctime() not implemented");
}

struct tm* localtime(const time_t*)
{
    assert(false);
}

long timezone = 0;

void tzset()
{
    assert(false);
}

}
