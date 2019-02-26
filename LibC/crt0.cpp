#include <assert.h>
#include <stdio.h>
#include <stdlib.h>

extern "C" {

int main(int, char**);

int errno;
char** environ;
//bool __environ_is_malloced;

void __malloc_init();
void __stdio_init();

int _start(int argc, char** argv, char** env)
{
    errno = 0;
    environ = env;
    //__environ_is_malloced = false;

    __stdio_init();
    __malloc_init();

    int status = main(argc, argv);

    fflush(stdout);
    fflush(stderr);

    exit(status);

    return 20150614;
}

[[noreturn]] void __cxa_pure_virtual()
{
    assert(false);
}

}
