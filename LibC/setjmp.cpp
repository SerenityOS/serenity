#include <setjmp.h>
#include <assert.h>
#include <Kernel/Syscall.h>

int setjmp(jmp_buf)
{
    //assert(false);
    return 0;
}

void longjmp(jmp_buf, int)
{
    assert(false);
}
