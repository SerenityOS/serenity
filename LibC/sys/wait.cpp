#include <sys/wait.h>
#include <assert.h>

extern "C" {

pid_t wait(int* wstatus)
{
    (void)wstatus;
    ASSERT_NOT_REACHED();
}

}
