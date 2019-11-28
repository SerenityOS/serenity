#include <Kernel/kstdio.h>

extern "C" void outside_func();

extern "C" void module_init()
{
    kprintf("TestModule has booted!\n");

    for (int i = 0; i < 99; ++i) {
        kprintf("i is now %d\n", i);
    }
}
