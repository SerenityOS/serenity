#include <assert.h>
#include <ulimit.h>

extern "C" {

long ulimit(int cmd, long newlimit)
{
    (void)cmd;
    (void)newlimit;
    ASSERT_NOT_REACHED();
    return -1;
}
}
