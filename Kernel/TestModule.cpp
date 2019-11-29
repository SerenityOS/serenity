#include <Kernel/kstdio.h>
#include <Kernel/Process.h>

extern "C" const char module_name[] = "TestModule";

extern "C" void module_init()
{
    kprintf("TestModule has booted!\n");

    for (int i = 0; i < 3; ++i) {
        kprintf("i is now %d\n", i);
    }

    kprintf("current pid: %d\n", current->process().sys$getpid());
    kprintf("current process name: %s\n", current->process().name().characters());
}

extern "C" void module_fini()
{
    kprintf("TestModule is being removed!\n");
}
