#include "unistd.h"
#include <Kernel/Syscall.h>

extern "C" {

uid_t getuid()
{
    return Syscall::invoke(Syscall::PosixGetuid);
}

uid_t getgid()
{
    return Syscall::invoke(Syscall::PosixGetgid);
}

uid_t getpid()
{
    return Syscall::invoke(Syscall::PosixGetpid);
}

}

