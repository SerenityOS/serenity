#include <stdio.h>
#include <string.h>
#include <Kernel/Syscall.h>
#include <AK/StringImpl.h>

extern "C" int main(int, char**);

FILE __default_streams[3];

int errno;
FILE* stdin;
FILE* stdout;
FILE* stderr;
char** environ;

extern "C" void __malloc_init();

extern "C" int _start()
{
    __malloc_init();

    errno = 0;

    memset(__default_streams, 0, sizeof(__default_streams));

    __default_streams[0].fd = 0;
    stdin = &__default_streams[0];

    __default_streams[1].fd = 1;
    stdout = &__default_streams[1];

    __default_streams[2].fd = 2;
    stderr = &__default_streams[2];

    StringImpl::initializeGlobals();

    int status = 254;
    int argc;
    char** argv;
    int rc = Syscall::invoke(Syscall::SC_get_arguments, (dword)&argc, (dword)&argv);
    if (rc < 0)
        goto epilogue;
    rc = Syscall::invoke(Syscall::SC_get_environment, (dword)&environ);
    if (rc < 0)
        goto epilogue;
    status = main(argc, argv);

epilogue:
    Syscall::invoke(Syscall::SC_exit, status);

    // Birger's birthday <3
    return 20150614;
}
