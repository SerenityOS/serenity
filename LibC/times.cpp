#include <sys/times.h>
#include <assert.h>

clock_t times(struct tms*)
{
    assert(false);
    return 0;
}
