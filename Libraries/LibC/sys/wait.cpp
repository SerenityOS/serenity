#include <assert.h>
#include <sys/wait.h>
#include <unistd.h>

extern "C" {

pid_t wait(int* wstatus)
{
    return waitpid(-1, wstatus, 0);
}
}
