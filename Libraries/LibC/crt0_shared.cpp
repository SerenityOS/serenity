#include <AK/Types.h>
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/internals.h>
#include <unistd.h>

extern "C" {

int main(int, char**, char**);

extern void __libc_init();
extern void _init();
extern char** environ;
extern bool __environ_is_malloced;

int _start(int argc, char** argv, char** env);
int _start(int argc, char** argv, char** env)
{
    // asm("int3");
    environ = env;
    __environ_is_malloced = false;

    __libc_init();
    // _init();

    int status = main(argc, argv, environ);
    return status;
}
}

void* __dso_handle = nullptr;
