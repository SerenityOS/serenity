#include <unistd.h>
#include <string.h>
#include <errno.h>
#include <stdarg.h>
#include <assert.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <fcntl.h>
#include <Kernel/Syscall.h>
#include <AK/Vector.h>
#include <AK/AKString.h>

extern "C" {

int systrace(pid_t pid)
{
    int rc = syscall(SC_systrace, pid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int chown(const char* pathname, uid_t uid, gid_t gid)
{
    int rc = syscall(SC_chown, pathname, uid, gid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

pid_t fork()
{
    int rc = syscall(SC_fork);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int execv(const char* path, char* const argv[])
{
    return execve(path, argv, environ);
}

int execve(const char* filename, char* const argv[], char* const envp[])
{
    int rc = syscall(SC_execve, filename, argv, envp);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int execvpe(const char* filename, char* const argv[], char* const envp[])
{
    int rc = execve(filename, argv, environ);
    if (rc < 0 && errno != ENOENT) {
        fprintf(stderr, "execvpe() failed on first with %s\n", strerror(errno));
        return rc;
    }
    String path = getenv("PATH");
    if (path.is_empty())
        path = "/bin:/usr/bin";
    auto parts = path.split(':');
    for (auto& part : parts) {
        auto candidate = String::format("%s/%s", part.characters(), filename);
        int rc = execve(candidate.characters(), argv, environ);
        if (rc < 0 && errno != ENOENT) {
            printf("execvpe() failed on attempt (%s) with %s\n", candidate.characters(), strerror(errno));
            return rc;
        }
    }
    errno = ENOENT;
    return -1;
}

int execvp(const char* filename, char* const argv[])
{
    return execvpe(filename, argv, environ);
}

int execl(const char* filename, const char* arg0, ...)
{
    Vector<const char*, 16> args;
    args.append(arg0);

    va_list ap;
    va_start(ap, arg0);
    for (;;) {
        const char* arg = va_arg(ap, const char*);
        if (!arg)
            break;
        args.append(arg);
    }
    va_end(ap);
    args.append(nullptr);
    return execve(filename, const_cast<char* const*>(args.data()), environ);
}

uid_t getuid()
{
    return syscall(SC_getuid);
}

gid_t getgid()
{
    return syscall(SC_getgid);
}

uid_t geteuid()
{
    return syscall(SC_geteuid);
}

gid_t getegid()
{
    return syscall(SC_getegid);
}

pid_t getpid()
{
    return syscall(SC_getpid);
}

pid_t getppid()
{
    return syscall(SC_getppid);
}

pid_t setsid()
{
    int rc = syscall(SC_setsid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

pid_t tcgetpgrp(int fd)
{
    return ioctl(fd, TIOCGPGRP);
}

int tcsetpgrp(int fd, pid_t pgid)
{
    return ioctl(fd, TIOCSPGRP, pgid);
}

int setpgid(pid_t pid, pid_t pgid)
{
    int rc = syscall(SC_setpgid, pid, pgid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

pid_t getpgid(pid_t pid)
{
    int rc = syscall(SC_getpgid, pid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

pid_t getpgrp()
{
    int rc = syscall(SC_getpgrp);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int creat(const char* path, mode_t mode)
{
    return open(path, O_CREAT | O_WRONLY | O_TRUNC, mode);
}

int open(const char* path, int options, ...)
{
    va_list ap;
    va_start(ap, options);
    int rc = syscall(SC_open, path, options, (mode_t)va_arg(ap, unsigned));
    va_end(ap);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

ssize_t read(int fd, void* buf, size_t count)
{
    int rc = syscall(SC_read, fd, buf, count);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

ssize_t write(int fd, const void* buf, size_t count)
{
    int rc = syscall(SC_write, fd, buf, count);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int ttyname_r(int fd, char* buffer, size_t size)
{
    int rc = syscall(SC_ttyname_r, fd, buffer, size);
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
    int rc = syscall(SC_close, fd);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

pid_t waitpid(pid_t waitee, int* wstatus, int options)
{
    int rc = syscall(SC_waitpid, waitee, wstatus, options);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int lstat(const char* path, struct stat* statbuf)
{
    int rc = syscall(SC_lstat, path, statbuf);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int stat(const char* path, struct stat* statbuf)
{
    int rc = syscall(SC_stat, path, statbuf);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int fstat(int fd, struct stat *statbuf)
{
    int rc = syscall(SC_fstat, fd, statbuf);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int chdir(const char* path)
{
    int rc = syscall(SC_chdir, path);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

char* getcwd(char* buffer, size_t size)
{
    if (!buffer) {
        size = size ? size : PATH_MAX;
        buffer = (char*)malloc(size);
    }
    int rc = syscall(SC_getcwd, buffer, size);
    __RETURN_WITH_ERRNO(rc, buffer, nullptr);
}

char* getwd(char* buf)
{
    auto* p = getcwd(buf, PATH_MAX);
    return p;
}

int sleep(unsigned seconds)
{
    return syscall(SC_sleep, seconds);
}

int usleep(useconds_t usec)
{
    return syscall(SC_usleep, usec);
}

int gethostname(char* buffer, size_t size)
{
    int rc = syscall(SC_gethostname, buffer, size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

ssize_t readlink(const char* path, char* buffer, size_t size)
{
    int rc = syscall(SC_readlink, path, buffer, size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

off_t lseek(int fd, off_t offset, int whence)
{
    int rc = syscall(SC_lseek, fd, offset, whence);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int link(const char* old_path, const char* new_path)
{
    int rc = syscall(SC_link, old_path, new_path);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int unlink(const char* pathname)
{
    int rc = syscall(SC_unlink, pathname);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int symlink(const char* target, const char* linkpath)
{
    int rc = syscall(SC_symlink, target, linkpath);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int rmdir(const char* pathname)
{
    int rc = syscall(SC_rmdir, pathname);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int isatty(int fd)
{
    int rc = syscall(SC_isatty, fd);
    __RETURN_WITH_ERRNO(rc, 1, 0);
}

int getdtablesize()
{
    int rc = syscall(SC_getdtablesize);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int dup(int old_fd)
{
    int rc = syscall(SC_dup, old_fd);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int dup2(int old_fd, int new_fd)
{
    int rc = syscall(SC_dup2, old_fd, new_fd);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int setgroups(size_t size, const gid_t* list)
{
    int rc = syscall(SC_getgroups, size, list);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int getgroups(int size, gid_t list[])
{
    int rc = syscall(SC_getgroups, size, list);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int pipe(int pipefd[2])
{
    int rc = syscall(SC_pipe, pipefd);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

unsigned int alarm(unsigned int seconds)
{
    return syscall(SC_alarm, seconds);
}

int setuid(uid_t uid)
{
    int rc = syscall(SC_setuid, uid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int setgid(uid_t gid)
{
    int rc = syscall(SC_setgid, gid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int access(const char* pathname, int mode)
{
    int rc = syscall(SC_access, pathname, mode);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int mknod(const char* pathname, mode_t mode, dev_t dev)
{
    int rc = syscall(SC_mknod, pathname, mode, dev);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

long fpathconf(int fd, int name)
{
    (void) fd;
    (void) name;
    ASSERT_NOT_REACHED();
}

long pathconf(const char* path, int name)
{
    (void) path;
    (void) name;
    ASSERT_NOT_REACHED();
}

void _exit(int status)
{
    syscall(SC_exit, status);
    ASSERT_NOT_REACHED();
}

void sync()
{
    syscall(SC_sync);
}

int read_tsc(unsigned* lsw, unsigned* msw)
{
    int rc = syscall(SC_read_tsc, lsw, msw);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int create_shared_buffer(pid_t peer_pid, int size, void** buffer)
{
    int rc = syscall(SC_create_shared_buffer, peer_pid, size, buffer);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

void* get_shared_buffer(int shared_buffer_id)
{
    int rc = syscall(SC_get_shared_buffer, shared_buffer_id);
    if (rc < 0 && -rc < EMAXERRNO) {
        errno = -rc;
        return (void*)-1;
    }
    return (void*)rc;
}

int release_shared_buffer(int shared_buffer_id)
{
    int rc = syscall(SC_release_shared_buffer, shared_buffer_id);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int get_shared_buffer_size(int shared_buffer_id)
{
    int rc = syscall(SC_get_shared_buffer_size, shared_buffer_id);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int seal_shared_buffer(int shared_buffer_id)
{
    int rc = syscall(SC_seal_shared_buffer, shared_buffer_id);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

char* getlogin()
{
    static char __getlogin_buffer[256];
    if (auto* passwd = getpwuid(getuid())) {
        strncpy(__getlogin_buffer, passwd->pw_name, sizeof(__getlogin_buffer));
        return __getlogin_buffer;
    }
    return nullptr;
}

int create_thread(int(*entry)(void*), void* argument)
{
    int rc = syscall(SC_create_thread, entry, argument);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

void exit_thread(int code)
{
    syscall(SC_exit_thread, code);
    ASSERT_NOT_REACHED();
}

int ftruncate(int fd, off_t length)
{
    int rc = syscall(SC_ftruncate, fd, length);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gettid()
{
    int rc = syscall(SC_gettid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int donate(int tid)
{
    int rc = syscall(SC_donate, tid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

void sysbeep()
{
    syscall(SC_beep);
}

int fsync(int fd)
{
    UNUSED_PARAM(fd);
    dbgprintf("FIXME: Implement fsync()\n");
    return 0;
}

}
