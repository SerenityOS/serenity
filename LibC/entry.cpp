#include <stdio.h>
#include <string.h>
#include <Kernel/Syscall.h>
#include <AK/StringImpl.h>

extern "C" int main(int, char**);

int errno;
char** environ;

extern "C" void __malloc_init();
extern "C" void __stdio_init();

extern "C" int _start()
{
    errno = 0;

    __stdio_init();
    __malloc_init();

    StringImpl::initialize_globals();

    int status = 254;
    int argc;
    char** argv;
    int rc = syscall(SC_get_arguments, &argc, &argv);
    if (rc < 0)
        goto epilogue;
    rc = syscall(SC_get_environment, &environ);
    if (rc < 0)
        goto epilogue;
    status = main(argc, argv);

    fflush(stdout);
    fflush(stderr);

epilogue:
    syscall(SC_exit, status);

    // Birger's birthday <3
    return 20150614;
}
