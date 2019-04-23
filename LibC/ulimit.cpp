#include <ulimit.h>
#include <assert.h>

extern "C" {

long ulimit(int cmd, long newlimit)
{
    (void) cmd;
    (void) newlimit;
    ASSERT_NOT_REACHED();
}

}
