#include "unistd.h"
#include "string.h"
#include "errno.h"
#include <Kernel/Syscall.h>

extern "C" {

pid_t fork()
{
    return Syscall::invoke(Syscall::PosixFork);
}

uid_t getuid()
{
    return Syscall::invoke(Syscall::PosixGetuid);
}

gid_t getgid()
{
    return Syscall::invoke(Syscall::PosixGetgid);
}

pid_t getpid()
{
    return Syscall::invoke(Syscall::PosixGetpid);
}

pid_t setsid()
{
    return Syscall::invoke(Syscall::PosixSetsid);
}

pid_t tcgetpgrp(int fd)
{
    return Syscall::invoke(Syscall::PosixTcgetpgrp, (dword)fd);
}

int tcsetpgrp(int fd, pid_t pgid)
{
    return Syscall::invoke(Syscall::PosixTcsetpgrp, (dword)fd, (dword)pgid);
}

int setpgid(pid_t pid, pid_t pgid)
{
    return Syscall::invoke(Syscall::PosixSetpgid, (dword)pid, (dword)pgid);
}

pid_t getpgid(pid_t pid)
{
    return Syscall::invoke(Syscall::PosixGetpgid, (dword)pid);
}

pid_t getpgrp()
{
    return Syscall::invoke(Syscall::PosixGetpgrp);
}

int open(const char* path, int options)
{
    int rc = Syscall::invoke(Syscall::PosixOpen, (dword)path, (dword)options);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

ssize_t read(int fd, void* buf, size_t count)
{
    int rc = Syscall::invoke(Syscall::PosixRead, (dword)fd, (dword)buf, (dword)count);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

ssize_t write(int fd, const void* buf, size_t count)
{
    int rc = Syscall::invoke(Syscall::PosixWrite, (dword)fd, (dword)buf, (dword)count);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int ttyname_r(int fd, char* buffer, size_t size)
{
    int rc = Syscall::invoke(Syscall::PosixTtynameR, (dword)fd, (dword)buffer, (dword)size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

static char ttyname_buf[32];
char* ttyname(int fd)
{
    if (ttyname_r(fd, ttyname_buf, sizeof(ttyname_buf)) < 0)
        return nullptr;
    return ttyname_buf;
}

int close(int fd)
{
    int rc = Syscall::invoke(Syscall::PosixClose, fd);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

pid_t waitpid(pid_t waitee, int* wstatus, int options)
{
    int rc = Syscall::invoke(Syscall::PosixWaitpid, waitee, (dword)wstatus);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int lstat(const char* path, struct stat* statbuf)
{
    int rc = Syscall::invoke(Syscall::PosixLstat, (dword)path, (dword)statbuf);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int stat(const char* path, struct stat* statbuf)
{
    int rc = Syscall::invoke(Syscall::PosixStat, (dword)path, (dword)statbuf);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int chdir(const char* path)
{
    int rc = Syscall::invoke(Syscall::PosixChdir, (dword)path);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

char* getcwd(char* buffer, size_t size)
{
    int rc = Syscall::invoke(Syscall::PosixGetcwd, (dword)buffer, (dword)size);
    __RETURN_WITH_ERRNO(rc, buffer, nullptr);
}

int sleep(unsigned seconds)
{
    return Syscall::invoke(Syscall::Sleep, (dword)seconds);
}

int gethostname(char* buffer, size_t size)
{
    int rc = Syscall::invoke(Syscall::PosixGethostname, (dword)buffer, (dword)size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

ssize_t readlink(const char* path, char* buffer, size_t size)
{
    int rc = Syscall::invoke(Syscall::PosixReadlink, (dword)path, (dword)buffer, (dword)size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

off_t lseek(int fd, off_t offset, int whence)
{
    int rc = Syscall::invoke(Syscall::PosixLseek, (dword)fd, (dword)offset, (dword)whence);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

}

