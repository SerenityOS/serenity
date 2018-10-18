#include "Userspace.h"
#include "Syscall.h"
#include "StdLib.h"

namespace Userspace {

int strlen(const char* str)
{
    int len = 0;
    while (*(str++))
        ++len;
    return len;
}

int open(const char* path)
{
    return DO_SYSCALL_A2(Syscall::PosixOpen, path, strlen(path));
}

int close(int fd)
{
    return DO_SYSCALL_A1(Syscall::PosixClose, fd);
}

int read(int fd, void* outbuf, size_t nread)
{
    return DO_SYSCALL_A3(Syscall::PosixRead, fd, outbuf, nread);
}

int seek(int fd, int offset)
{
    return DO_SYSCALL_A2(Syscall::PosixRead, fd, offset);
}

int kill(pid_t pid, int sig)
{
    return DO_SYSCALL_A2(Syscall::PosixKill, pid, sig);
}

uid_t getuid()
{
    return DO_SYSCALL_A0(Syscall::PosixGetuid);
}

void sleep(DWORD ticks)
{
    DO_SYSCALL_A1(Syscall::Sleep, ticks);
}

void yield()
{
    DO_SYSCALL_A0(Syscall::Yield);
}

}
