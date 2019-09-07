#include <assert.h>
#include <stdio.h>
#include <stdlib.h>

extern "C" {

int main(int, char**);

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

int _start(int argc, char** argv, char** env)
{
    environ = env;
    __environ_is_malloced = false;

    __libc_init();

    extern void _init();
    _init();

    extern void (*__init_array_start[])(int, char**, char**) __attribute__((visibility("hidden")));
    extern void (*__init_array_end[])(int, char**, char**) __attribute__((visibility("hidden")));

    const size_t size = __init_array_end - __init_array_start;
    for (size_t i = 0; i < size; i++)
        (*__init_array_start[i])(argc, argv, env);

    int status = main(argc, argv);

    exit(status);

    return 20150614;
}

[[noreturn]] void __cxa_pure_virtual()
{
    ASSERT_NOT_REACHED();
}

void __cxa_atexit()
{
}
}
