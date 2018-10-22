#include <Kernel/Syscall.h>

extern "C" int main(int, char**);

extern "C" int _start()
{
    // FIXME: Pass appropriate argc/argv.
    int status = main(0, nullptr);

    Syscall::invoke(Syscall::PosixExit, status);

    // Birger's birthday <3
    return 20150614;
}
