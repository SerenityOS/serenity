#include <Kernel/kstdio.h>

extern "C" void module_init()
{
    kprintf("TestModule has booted!\n");

    for (int i = 0; i < 99; ++i) {
        kprintf("i is now %d\n", i);
    }
}

extern "C" void module_fini()
{
    kprintf("TestModule is being removed!\n");
}
