#include <AK/Types.h>
#include <assert.h>

extern "C" {

__thread int errno;
char** environ;
bool __environ_is_malloced;

void __libc_init()
{
    void __malloc_init();
    __malloc_init();

    void __stdio_init();
    __stdio_init();
}

[[noreturn]] void __cxa_pure_virtual() __attribute__((weak));

[[noreturn]] void __cxa_pure_virtual()
{
    ASSERT_NOT_REACHED();
}

extern u32 __stack_chk_guard;
u32 __stack_chk_guard = (u32)0xc0000c13;

[[noreturn]] void __stack_chk_fail()
{
    ASSERT_NOT_REACHED();
}

}
