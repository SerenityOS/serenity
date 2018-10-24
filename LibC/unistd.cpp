#include "unistd.h"
#include "string.h"
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

int open(const char* path)
{
    size_t length = strlen(path);
    return Syscall::invoke(Syscall::PosixOpen, (dword)path, (dword)length);
}

ssize_t read(int fd, void* buf, size_t count)
{
    return Syscall::invoke(Syscall::PosixRead, (dword)fd, (dword)buf, (dword)count);
}

int close(int fd)
{
    return Syscall::invoke(Syscall::PosixClose, fd);
}

pid_t waitpid(pid_t waitee)
{
    return Syscall::invoke(Syscall::PosixWaitpid, waitee);
}

int lstat(const char* path, stat* statbuf)
{
    return Syscall::invoke(Syscall::PosixLstat, (dword)path, (dword)statbuf);
}

}

