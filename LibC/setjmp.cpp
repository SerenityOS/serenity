#include <setjmp.h>
#include <assert.h>
#include <Kernel/Syscall.h>

int setjmp(jmp_buf)
{
    assert(false);
}

void longjmp(jmp_buf, int)
{
    assert(false);
}
