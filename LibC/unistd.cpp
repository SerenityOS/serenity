#include <unistd.h>
#include <string.h>
#include <errno.h>
#include <stdarg.h>
#include <assert.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <Kernel/Syscall.h>

extern "C" {

pid_t fork()
{
    int rc = Syscall::invoke(Syscall::SC_fork);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int execve(const char* filename, const char** argv, const char** envp)
{
    int rc = Syscall::invoke(Syscall::SC_execve, (dword)filename, (dword)argv, (dword)envp);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

uid_t getuid()
{
    return Syscall::invoke(Syscall::SC_getuid);
}

gid_t getgid()
{
    return Syscall::invoke(Syscall::SC_getgid);
}

uid_t geteuid()
{
    return Syscall::invoke(Syscall::SC_geteuid);
}

gid_t getegid()
{
    return Syscall::invoke(Syscall::SC_getegid);
}

pid_t getpid()
{
    return Syscall::invoke(Syscall::SC_getpid);
}

pid_t getppid()
{
    return Syscall::invoke(Syscall::SC_getppid);
}

pid_t setsid()
{
    int rc = Syscall::invoke(Syscall::SC_setsid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

pid_t tcgetpgrp(int fd)
{
    int rc = Syscall::invoke(Syscall::SC_tcgetpgrp, (dword)fd);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int tcsetpgrp(int fd, pid_t pgid)
{
    int rc = Syscall::invoke(Syscall::SC_tcsetpgrp, (dword)fd, (dword)pgid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int setpgid(pid_t pid, pid_t pgid)
{
    int rc = Syscall::invoke(Syscall::SC_setpgid, (dword)pid, (dword)pgid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

pid_t getpgid(pid_t pid)
{
    int rc = Syscall::invoke(Syscall::SC_getpgid, (dword)pid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

pid_t getpgrp()
{
    int rc = Syscall::invoke(Syscall::SC_getpgrp);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int open(const char* path, int options, ...)
{
    va_list ap;
    va_start(ap, options);
    int rc = Syscall::invoke(Syscall::SC_open, (dword)path, (dword)options, (dword)ap);
    va_end(ap);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

ssize_t read(int fd, void* buf, size_t count)
{
    int rc = Syscall::invoke(Syscall::SC_read, (dword)fd, (dword)buf, (dword)count);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

ssize_t write(int fd, const void* buf, size_t count)
{
    int rc = Syscall::invoke(Syscall::SC_write, (dword)fd, (dword)buf, (dword)count);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int ttyname_r(int fd, char* buffer, size_t size)
{
    int rc = Syscall::invoke(Syscall::SC_ttyname_r, (dword)fd, (dword)buffer, (dword)size);
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
    int rc = Syscall::invoke(Syscall::SC_close, fd);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

pid_t waitpid(pid_t waitee, int* wstatus, int options)
{
    int rc = Syscall::invoke(Syscall::SC_waitpid, waitee, (dword)wstatus, (dword)options);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int lstat(const char* path, struct stat* statbuf)
{
    int rc = Syscall::invoke(Syscall::SC_lstat, (dword)path, (dword)statbuf);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int stat(const char* path, struct stat* statbuf)
{
    int rc = Syscall::invoke(Syscall::SC_stat, (dword)path, (dword)statbuf);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int fstat(int fd, struct stat *statbuf)
{
    int rc = Syscall::invoke(Syscall::SC_fstat, (dword)fd, (dword)statbuf);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int chdir(const char* path)
{
    int rc = Syscall::invoke(Syscall::SC_chdir, (dword)path);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

char* getcwd(char* buffer, size_t size)
{
    int rc = Syscall::invoke(Syscall::SC_getcwd, (dword)buffer, (dword)size);
    __RETURN_WITH_ERRNO(rc, buffer, nullptr);
}

char* getwd(char* buf)
{
    auto* p = getcwd(buf, PATH_MAX);
    return p;
}

int sleep(unsigned seconds)
{
    return Syscall::invoke(Syscall::SC_sleep, (dword)seconds);
}

int gethostname(char* buffer, size_t size)
{
    int rc = Syscall::invoke(Syscall::SC_gethostname, (dword)buffer, (dword)size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

ssize_t readlink(const char* path, char* buffer, size_t size)
{
    int rc = Syscall::invoke(Syscall::SC_readlink, (dword)path, (dword)buffer, (dword)size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

off_t lseek(int fd, off_t offset, int whence)
{
    int rc = Syscall::invoke(Syscall::SC_lseek, (dword)fd, (dword)offset, (dword)whence);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int link(const char*, const char*)
{
    assert(false);
}

int unlink(const char*)
{
    assert(false);
}

int isatty(int fd)
{
    int rc = Syscall::invoke(Syscall::SC_isatty, (dword)fd);
    __RETURN_WITH_ERRNO(rc, 1, 0);
}

int getdtablesize()
{
    int rc = Syscall::invoke(Syscall::SC_getdtablesize);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int dup(int old_fd)
{
    int rc = Syscall::invoke(Syscall::SC_dup, (dword)old_fd);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int dup2(int old_fd, int new_fd)
{
    int rc = Syscall::invoke(Syscall::SC_dup2, (dword)old_fd, (dword)new_fd);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int setgroups(size_t size, const gid_t* list)
{
    int rc = Syscall::invoke(Syscall::SC_getgroups, (dword)size, (dword)list);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int getgroups(int size, gid_t list[])
{
    int rc = Syscall::invoke(Syscall::SC_getgroups, (dword)size, (dword)list);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int pipe(int pipefd[2])
{
    int rc = Syscall::invoke(Syscall::SC_pipe, (dword)pipefd);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

unsigned int alarm(unsigned int seconds)
{
    return Syscall::invoke(Syscall::SC_alarm, (dword)seconds);
}

int setuid(uid_t uid)
{
    int rc = Syscall::invoke(Syscall::SC_setuid, (dword)uid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int setgid(uid_t gid)
{
    int rc = Syscall::invoke(Syscall::SC_setgid, (dword)gid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int access(const char* pathname, int mode)
{
    int rc = Syscall::invoke(Syscall::SC_access, (dword)pathname, (dword)mode);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

}
