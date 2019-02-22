#include <stdio.h>
#include <string.h>
#include <Kernel/Syscall.h>
#include <AK/StringImpl.h>

extern "C" {

int main(int, char**);

int errno;
char** environ;

void __malloc_init();
void __stdio_init();

int _start(int argc, char** argv, char** env)
{
    errno = 0;
    environ = env;

    __stdio_init();
    __malloc_init();

    int status = main(argc, argv);

    fflush(stdout);
    fflush(stderr);

    syscall(SC_exit, status);

    // Birger's birthday <3
    return 20150614;
}

[[noreturn]] void __cxa_pure_virtual()
{
    ASSERT_NOT_REACHED();
}

}
