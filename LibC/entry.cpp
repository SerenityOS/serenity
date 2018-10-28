#include <Kernel/Syscall.h>
#include <AK/StringImpl.h>

extern "C" int main(int, char**);

int errno;

extern "C" int _start()
{
    errno = 0;

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
