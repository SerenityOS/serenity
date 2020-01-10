#include <AK/ScopedValueRollback.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <Kernel/Syscall.h>
#include <alloca.h>
#include <assert.h>
#include <errno.h>
#include <fcntl.h>
#include <grp.h>
#include <pwd.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <termios.h>
#include <unistd.h>

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

int fchown(int fd, uid_t uid, gid_t gid)
{
    int rc = syscall(SC_fchown, fd, uid, gid);
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
    size_t arg_count = 0;
    for (size_t i = 0; argv[i]; ++i)
        ++arg_count;

    size_t env_count = 0;
    for (size_t i = 0; envp[i]; ++i)
        ++env_count;

    auto copy_strings = [&](auto& vec, size_t count, auto& output) {
        output.length = count;
        for (size_t i = 0; vec[i]; ++i) {
            output.strings[i].characters = vec[i];
            output.strings[i].length = strlen(vec[i]);
        }
    };

    Syscall::SC_execve_params params;
    params.arguments.strings = (Syscall::StringArgument*)alloca(arg_count * sizeof(Syscall::StringArgument));
    params.environment.strings = (Syscall::StringArgument*)alloca(env_count * sizeof(Syscall::StringArgument));

    params.path = { filename, strlen(filename) };
    copy_strings(argv, arg_count, params.arguments);
    copy_strings(envp, env_count, params.environment);

    int rc = syscall(SC_execve, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int execvpe(const char* filename, char* const argv[], char* const envp[])
{
    ScopedValueRollback errno_rollback(errno);
    int rc = execve(filename, argv, envp);
    if (rc < 0 && errno != ENOENT) {
        errno_rollback.set_override_rollback_value(errno);
        dbg() << "execvpe() failed on first with" << strerror(errno);
        return rc;
    }
    String path = getenv("PATH");
    if (path.is_empty())
        path = "/bin:/usr/bin";
    auto parts = path.split(':');
    for (auto& part : parts) {
        auto candidate = String::format("%s/%s", part.characters(), filename);
        int rc = execve(candidate.characters(), argv, envp);
        if (rc < 0 && errno != ENOENT) {
            errno_rollback.set_override_rollback_value(errno);
            dbg() << "execvpe() failed on attempt (" << candidate << ") with " << strerror(errno);
            return rc;
        }
    }
    errno_rollback.set_override_rollback_value(ENOENT);
    dbg() << "execvpe() leaving :(";
    return -1;
}

int execvp(const char* filename, char* const argv[])
{
    int rc = execvpe(filename, argv, environ);
    dbg() << "execvp() about to return " << rc << " with errno=" << errno;
    return rc;
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

int execlp(const char* filename, const char* arg0, ...)
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
    return execvpe(filename, const_cast<char* const*>(args.data()), environ);
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
    if (!path) {
        errno = EFAULT;
        return -1;
    }
    int rc = syscall(SC_lstat, path, strlen(path), statbuf);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int stat(const char* path, struct stat* statbuf)
{
    if (!path) {
        errno = EFAULT;
        return -1;
    }
    int rc = syscall(SC_stat, path, strlen(path), statbuf);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int fstat(int fd, struct stat* statbuf)
{
    int rc = syscall(SC_fstat, fd, statbuf);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int chdir(const char* path)
{
    if (!path) {
        errno = EFAULT;
        return -1;
    }
    int rc = syscall(SC_chdir, path, strlen(path));
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int fchdir(int fd)
{
    int rc = syscall(SC_fchdir, fd);
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
    Syscall::SC_readlink_params params { { path, strlen(path) }, { buffer, size } };
    int rc = syscall(SC_readlink, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

off_t lseek(int fd, off_t offset, int whence)
{
    int rc = syscall(SC_lseek, fd, offset, whence);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int link(const char* old_path, const char* new_path)
{
    if (!old_path || !new_path) {
        errno = EFAULT;
        return -1;
    }
    Syscall::SC_link_params params { { old_path, strlen(old_path) }, { new_path, strlen(new_path) } };
    int rc = syscall(SC_link, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int unlink(const char* pathname)
{
    int rc = syscall(SC_unlink, pathname, strlen(pathname));
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int symlink(const char* target, const char* linkpath)
{
    int rc = syscall(SC_symlink, target, linkpath);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int rmdir(const char* pathname)
{
    if (!pathname) {
        errno = EFAULT;
        return -1;
    }
    int rc = syscall(SC_rmdir, pathname, strlen(pathname));
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int isatty(int fd)
{
    struct termios dummy;
    return tcgetattr(fd, &dummy) == 0;
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
    int rc = syscall(SC_setgroups, size, list);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int getgroups(int size, gid_t list[])
{
    int rc = syscall(SC_getgroups, size, list);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int pipe(int pipefd[2])
{
    return pipe2(pipefd, 0);
}

int pipe2(int pipefd[2], int flags)
{
    int rc = syscall(SC_pipe, pipefd, flags);
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
    if (!pathname) {
        errno = EFAULT;
        return -1;
    }
    int rc = syscall(SC_access, pathname, strlen(pathname), mode);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int mknod(const char* pathname, mode_t mode, dev_t dev)
{
    int rc = syscall(SC_mknod, pathname, mode, dev);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

long fpathconf(int fd, int name)
{
    (void)fd;
    (void)name;

    switch (name) {
    case _PC_PATH_MAX:
        return PATH_MAX;
    case _PC_VDISABLE:
        return _POSIX_VDISABLE;
    }

    ASSERT_NOT_REACHED();
}

long pathconf(const char* path, int name)
{
    (void)path;

    switch (name) {
    case _PC_PATH_MAX:
        return PATH_MAX;
    case _PC_PIPE_BUF:
        return PIPE_BUF;
    }

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

int create_shared_buffer(int size, void** buffer)
{
    int rc = syscall(SC_create_shared_buffer, size, buffer);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int share_buffer_with(int shared_buffer_id, pid_t peer_pid)
{
    int rc = syscall(SC_share_buffer_with, shared_buffer_id, peer_pid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int share_buffer_globally(int shared_buffer_id)
{
    int rc = syscall(SC_share_buffer_globally, shared_buffer_id);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int set_process_icon(int icon_id)
{
    int rc = syscall(SC_set_process_icon, icon_id);
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
        endpwent();
        return __getlogin_buffer;
    }
    endpwent();
    return nullptr;
}

int ftruncate(int fd, off_t length)
{
    int rc = syscall(SC_ftruncate, fd, length);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int gettid()
{
    return syscall(SC_gettid);
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

int halt()
{
    int rc = syscall(SC_halt);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int reboot()
{
    int rc = syscall(SC_reboot);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int mount(const char* device, const char* mountpoint, const char* fstype)
{
    int rc = syscall(SC_mount, device, mountpoint, fstype);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int umount(const char* mountpoint)
{
    int rc = syscall(SC_umount, mountpoint, strlen(mountpoint));
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

void dump_backtrace()
{
    syscall(SC_dump_backtrace);
}

int get_process_name(char* buffer, int buffer_size)
{
    int rc = syscall(SC_get_process_name, buffer, buffer_size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int chroot(const char* path)
{
    if (!path) {
        errno = EFAULT;
        return -1;
    }
    int rc = syscall(SC_chroot, path, strlen(path));
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
