#include <Kernel/Syscall.h>

extern "C" int main(int, char**);

int errno;

extern "C" int _start()
{
    errno = 0;

    // FIXME: Pass appropriate argc/argv.
    int status = main(0, nullptr);

    Syscall::invoke(Syscall::PosixExit, status);

    // Birger's birthday <3
    return 20150614;
}
