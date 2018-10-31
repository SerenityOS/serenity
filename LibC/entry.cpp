#include <stdio.h>
#include <Kernel/Syscall.h>
#include <AK/StringImpl.h>

extern "C" int main(int, char**);

FILE __default_streams[3];

int errno;
FILE* stdin;
FILE* stdout;
FILE* stderr;

extern "C" int _start()
{
    errno = 0;

    __default_streams[0].fd = 0;
    stdin = &__default_streams[0];

    __default_streams[1].fd = 1;
    stdout = &__default_streams[1];

    __default_streams[2].fd = 2;
    stderr = &__default_streams[2];

    StringImpl::initializeGlobals();

    int argc;
    char** argv;
    int rc = Syscall::invoke(Syscall::GetArguments, (dword)&argc, (dword)&argv);
    int status = 254;
    if (rc == 0)
        status = main(argc, argv);
    Syscall::invoke(Syscall::PosixExit, status);

    // Birger's birthday <3
    return 20150614;
}
