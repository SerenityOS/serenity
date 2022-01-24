/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopedValueRollback.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <alloca.h>
#include <assert.h>
#include <bits/pthread_integration.h>
#include <errno.h>
#include <fcntl.h>
#include <getopt.h>
#include <grp.h>
#include <pwd.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/select.h>
#include <sys/types.h>
#include <syscall.h>
#include <termios.h>
#include <time.h>
#include <unistd.h>

extern "C" {

#ifdef NO_TLS
static int s_cached_tid = 0;
#else
static __thread int s_cached_tid = 0;
#endif

static int s_cached_pid = 0;

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/lchown.html
int lchown(const char* pathname, uid_t uid, gid_t gid)
{
    if (!pathname) {
        errno = EFAULT;
        return -1;
    }
    Syscall::SC_chown_params params { { pathname, strlen(pathname) }, uid, gid, AT_FDCWD, false };
    int rc = syscall(SC_chown, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/chown.html
int chown(const char* pathname, uid_t uid, gid_t gid)
{
    if (!pathname) {
        errno = EFAULT;
        return -1;
    }
    Syscall::SC_chown_params params { { pathname, strlen(pathname) }, uid, gid, AT_FDCWD, true };
    int rc = syscall(SC_chown, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fchown.html
int fchown(int fd, uid_t uid, gid_t gid)
{
    int rc = syscall(SC_fchown, fd, uid, gid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int fchownat(int fd, const char* pathname, uid_t uid, gid_t gid, int flags)
{
    if (!pathname) {
        errno = EFAULT;
        return -1;
    }
    Syscall::SC_chown_params params { { pathname, strlen(pathname) }, uid, gid, fd, !(flags & AT_SYMLINK_NOFOLLOW) };
    int rc = syscall(SC_chown, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fork.html
pid_t fork()
{
    __pthread_fork_prepare();

    int rc = syscall(SC_fork);
    if (rc == 0) {
        s_cached_tid = 0;
        s_cached_pid = 0;
        __pthread_fork_child();
    } else if (rc != -1) {
        __pthread_fork_parent();
    }
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/vfork.html
pid_t vfork()
{
    return fork();
}

// Non-POSIX, but present in BSDs and Linux
// https://man.openbsd.org/daemon.3
int daemon(int nochdir, int noclose)
{
    pid_t pid = fork();
    if (pid < 0)
        return -1;

    // exit parent, continue execution in child
    if (pid > 0)
        _exit(0);

    pid = setsid();
    if (pid < 0)
        return -1;

    if (nochdir == 0)
        (void)chdir("/");

    if (noclose == 0) {
        // redirect stdout and stderr to /dev/null
        int fd = open("/dev/null", O_WRONLY);
        if (fd < 0)
            return -1;
        (void)close(STDOUT_FILENO);
        (void)close(STDERR_FILENO);
        (void)dup2(fd, STDOUT_FILENO);
        (void)dup2(fd, STDERR_FILENO);
        (void)close(fd);
    }
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/execv.html
int execv(const char* path, char* const argv[])
{
    return execve(path, argv, environ);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/execve.html
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
    if (strchr(filename, '/'))
        return execve(filename, argv, envp);

    ScopedValueRollback errno_rollback(errno);
    String path = getenv("PATH");
    if (path.is_empty())
        path = "/bin:/usr/bin";
    auto parts = path.split(':');
    for (auto& part : parts) {
        auto candidate = String::formatted("{}/{}", part, filename);
        int rc = execve(candidate.characters(), argv, envp);
        if (rc < 0 && errno != ENOENT) {
            errno_rollback.set_override_rollback_value(errno);
            dbgln("execvpe() failed on attempt ({}) with {}", candidate, strerror(errno));
            return rc;
        }
    }
    errno_rollback.set_override_rollback_value(ENOENT);
    dbgln("execvpe() leaving :(");
    return -1;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/execvp.html
int execvp(const char* filename, char* const argv[])
{
    int rc = execvpe(filename, argv, environ);
    int saved_errno = errno;
    dbgln("execvp({}, ...) about to return {} with errno={}", filename, rc, saved_errno);
    errno = saved_errno;
    return rc;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/execl.html
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/execle.html
int execle(char const* filename, char const* arg0, ...)
{
    Vector<char const*> args;
    args.append(arg0);

    va_list ap;
    va_start(ap, arg0);
    char const* arg;
    do {
        arg = va_arg(ap, char const*);
        args.append(arg);
    } while (arg);

    auto argv = const_cast<char* const*>(args.data());
    auto envp = const_cast<char* const*>(va_arg(ap, char* const*));
    va_end(ap);

    return execve(filename, argv, envp);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/execlp.html
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getuid.html
uid_t geteuid()
{
    return syscall(SC_geteuid);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getegid.html
gid_t getegid()
{
    return syscall(SC_getegid);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getuid.html
uid_t getuid()
{
    return syscall(SC_getuid);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getgid.html
gid_t getgid()
{
    return syscall(SC_getgid);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getpid.html
pid_t getpid()
{
    int cached_pid = s_cached_pid;
    if (!cached_pid) {
        cached_pid = syscall(SC_getpid);
        s_cached_pid = cached_pid;
    }
    return cached_pid;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getppid.html
pid_t getppid()
{
    return syscall(SC_getppid);
}

int getresuid(uid_t* ruid, uid_t* euid, uid_t* suid)
{
    return syscall(SC_getresuid, ruid, euid, suid);
}

int getresgid(gid_t* rgid, gid_t* egid, gid_t* sgid)
{
    return syscall(SC_getresgid, rgid, egid, sgid);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getsid.html
pid_t getsid(pid_t pid)
{
    int rc = syscall(SC_getsid, pid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/setsid.html
pid_t setsid()
{
    int rc = syscall(SC_setsid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

pid_t tcgetpgrp(int fd)
{
    pid_t pgrp;
    int rc = ioctl(fd, TIOCGPGRP, &pgrp);
    if (rc < 0)
        return rc;
    return pgrp;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/tcgetpgrp.html
int tcsetpgrp(int fd, pid_t pgid)
{
    return ioctl(fd, TIOCSPGRP, pgid);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/setpgid.html
int setpgid(pid_t pid, pid_t pgid)
{
    int rc = syscall(SC_setpgid, pid, pgid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getpgid.html
pid_t getpgid(pid_t pid)
{
    int rc = syscall(SC_getpgid, pid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getpgrp.html
pid_t getpgrp()
{
    int rc = syscall(SC_getpgrp);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/read.html
ssize_t read(int fd, void* buf, size_t count)
{
    int rc = syscall(SC_read, fd, buf, count);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/pread.html
ssize_t pread(int fd, void* buf, size_t count, off_t offset)
{
    int rc = syscall(SC_pread, fd, buf, count, &offset);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/write.html
ssize_t write(int fd, const void* buf, size_t count)
{
    int rc = syscall(SC_write, fd, buf, count);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/pwrite.html
ssize_t pwrite(int fd, const void* buf, size_t count, off_t offset)
{
    // FIXME: This is not thread safe and should be implemented in the kernel instead.
    off_t old_offset = lseek(fd, 0, SEEK_CUR);
    lseek(fd, offset, SEEK_SET);
    ssize_t nwritten = write(fd, buf, count);
    lseek(fd, old_offset, SEEK_SET);
    return nwritten;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/ttyname_r.html
int ttyname_r(int fd, char* buffer, size_t size)
{
    int rc = syscall(SC_ttyname, fd, buffer, size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

static char ttyname_buf[32];
// https://pubs.opengroup.org/onlinepubs/9699919799/functions/ttyname.html
char* ttyname(int fd)
{
    if (ttyname_r(fd, ttyname_buf, sizeof(ttyname_buf)) < 0)
        return nullptr;
    return ttyname_buf;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/close.html
int close(int fd)
{
    int rc = syscall(SC_close, fd);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/chdir.html
int chdir(const char* path)
{
    if (!path) {
        errno = EFAULT;
        return -1;
    }
    int rc = syscall(SC_chdir, path, strlen(path));
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fchdir.html
int fchdir(int fd)
{
    int rc = syscall(SC_fchdir, fd);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getcwd.html
char* getcwd(char* buffer, size_t size)
{
    if (buffer && size == 0) {
        // POSIX requires that we set errno to EINVAL here, but in our syscall it makes sense to
        // allow "probing" the Kernel with a zero-sized buffer, and it does not return -EINVAL.
        // So we have to inject EINVAL here.
        errno = EINVAL;
        return nullptr;
    }

    bool self_allocated = false;
    if (!buffer) {
        size = size ? size : 64;
        buffer = (char*)malloc(size);
        self_allocated = true;
    }

    int rc = syscall(SC_getcwd, buffer, size);
    if (rc < 0) {
        if (self_allocated)
            free(buffer);
        errno = -rc;
        return nullptr;
    }

    size_t actual_size = static_cast<size_t>(rc);
    if (actual_size <= size) {
        return buffer;
    }

    // If we get here, the current directory path was silently truncated.

    if (!self_allocated) {
        // In this case, POSIX causes information loss: the caller cannot learn about the ideal
        // buffer size. This is the reason we went with silently truncation instead.
        errno = ERANGE;
        return nullptr;
    }

    // Try again.
    free(buffer);
    size = actual_size;
    buffer = (char*)malloc(size);
    rc = syscall(SC_getcwd, buffer, size);
    if (rc < 0) {
        // Can only happen if we lose a race. Let's pretend we lost the race in the first place.
        free(buffer);
        errno = -rc;
        return nullptr;
    }

    actual_size = static_cast<size_t>(rc);
    if (actual_size < size) {
        // If we're here, then cwd has become longer while we were looking at it. (Race with another thread?)
        // There's not much we can do, unless we want to loop endlessly
        // in this case. Let's leave it up to the caller whether to loop.
        free(buffer);
        errno = EAGAIN;
        return nullptr;
    }

    return buffer;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getwd.html
char* getwd(char* buf)
{
    if (buf == nullptr) {
        errno = EINVAL;
        return nullptr;
    }
    auto* p = getcwd(buf, PATH_MAX);
    if (errno == ERANGE) {
        // POSIX quirk
        errno = ENAMETOOLONG;
    }
    return p;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sleep.html
unsigned int sleep(unsigned int seconds)
{
    struct timespec ts = { seconds, 0 };
    if (clock_nanosleep(CLOCK_MONOTONIC_COARSE, 0, &ts, nullptr) < 0)
        return ts.tv_sec;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/usleep.html
int usleep(useconds_t usec)
{
    struct timespec ts = { (long)(usec / 1000000), (long)(usec % 1000000) * 1000 };
    return clock_nanosleep(CLOCK_MONOTONIC_COARSE, 0, &ts, nullptr);
}

int gethostname(char* buffer, size_t size)
{
    int rc = syscall(SC_gethostname, buffer, size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int sethostname(const char* hostname, ssize_t size)
{
    int rc = syscall(SC_sethostname, hostname, size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/readlink.html
ssize_t readlink(const char* path, char* buffer, size_t size)
{
    Syscall::SC_readlink_params params { { path, strlen(path) }, { buffer, size } };
    int rc = syscall(SC_readlink, &params);
    // Return the number of bytes placed in the buffer, not the full path size.
    __RETURN_WITH_ERRNO(rc, min((size_t)rc, size), -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/lseek.html
off_t lseek(int fd, off_t offset, int whence)
{
    int rc = syscall(SC_lseek, fd, &offset, whence);
    __RETURN_WITH_ERRNO(rc, offset, -1);
}
// https://pubs.opengroup.org/onlinepubs/9699919799/functions/link.html
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/unlink.html
int unlink(const char* pathname)
{
    int rc = syscall(SC_unlink, pathname, strlen(pathname));
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/symlink.html
int symlink(const char* target, const char* linkpath)
{
    if (!target || !linkpath) {
        errno = EFAULT;
        return -1;
    }
    Syscall::SC_symlink_params params { { target, strlen(target) }, { linkpath, strlen(linkpath) } };
    int rc = syscall(SC_symlink, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/rmdir.html
int rmdir(const char* pathname)
{
    if (!pathname) {
        errno = EFAULT;
        return -1;
    }
    int rc = syscall(SC_rmdir, pathname, strlen(pathname));
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/isatty.html
int isatty(int fd)
{
    return fcntl(fd, F_ISTTY);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/dup.html
int dup(int old_fd)
{
    return fcntl(old_fd, F_DUPFD, 0);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/dup2.html
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/pipe.html
int pipe(int pipefd[2])
{
    return pipe2(pipefd, 0);
}

//
int pipe2(int pipefd[2], int flags)
{
    int rc = syscall(SC_pipe, pipefd, flags);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

unsigned int alarm(unsigned int seconds)
{
    return syscall(SC_alarm, seconds);
}

int seteuid(uid_t euid)
{
    int rc = syscall(SC_seteuid, euid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int setegid(gid_t egid)
{
    int rc = syscall(SC_setegid, egid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int setuid(uid_t uid)
{
    int rc = syscall(SC_setuid, uid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int setgid(gid_t gid)
{
    int rc = syscall(SC_setgid, gid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int setreuid(uid_t ruid, uid_t euid)
{
    int rc = syscall(SC_setreuid, ruid, euid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int setresuid(uid_t ruid, uid_t euid, uid_t suid)
{
    int rc = syscall(SC_setresuid, ruid, euid, suid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int setresgid(gid_t rgid, gid_t egid, gid_t sgid)
{
    int rc = syscall(SC_setresgid, rgid, egid, sgid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/access.html
int access(const char* pathname, int mode)
{
    if (!pathname) {
        errno = EFAULT;
        return -1;
    }
    int rc = syscall(SC_access, pathname, strlen(pathname), mode);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mknod.html
int mknod(const char* pathname, mode_t mode, dev_t dev)
{
    if (!pathname) {
        errno = EFAULT;
        return -1;
    }
    Syscall::SC_mknod_params params { { pathname, strlen(pathname) }, mode, dev };
    int rc = syscall(SC_mknod, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fpathconf.html
long fpathconf([[maybe_unused]] int fd, int name)
{
    switch (name) {
    case _PC_NAME_MAX:
        return NAME_MAX;
    case _PC_PATH_MAX:
        return PATH_MAX;
    case _PC_VDISABLE:
        return _POSIX_VDISABLE;
    case _PC_LINK_MAX:
        return LINK_MAX;
    }

    VERIFY_NOT_REACHED();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/pathconf.html
long pathconf([[maybe_unused]] const char* path, int name)
{
    switch (name) {
    case _PC_NAME_MAX:
        return NAME_MAX;
    case _PC_PATH_MAX:
        return PATH_MAX;
    case _PC_PIPE_BUF:
        return PIPE_BUF;
    case _PC_LINK_MAX:
        return LINK_MAX;
    }

    VERIFY_NOT_REACHED();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/_exit.html
void _exit(int status)
{
    syscall(SC_exit, status);
    VERIFY_NOT_REACHED();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sync.html
void sync()
{
    syscall(SC_sync);
}

static String getlogin_buffer;

char* getlogin()
{
    if (getlogin_buffer.is_null()) {
        if (auto* passwd = getpwuid(getuid())) {
            getlogin_buffer = String(passwd->pw_name);
        }
        endpwent();
    }
    return const_cast<char*>(getlogin_buffer.characters());
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/ftruncate.html
int ftruncate(int fd, off_t length)
{
    int rc = syscall(SC_ftruncate, fd, &length);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/truncate.html
int truncate(const char* path, off_t length)
{
    int fd = open(path, O_RDWR | O_CREAT, 0666);
    if (fd < 0)
        return fd;
    int rc = ftruncate(fd, length);
    int saved_errno = errno;
    if (int close_rc = close(fd); close_rc < 0)
        return close_rc;
    errno = saved_errno;
    return rc;
}

int gettid()
{
    int cached_tid = s_cached_tid;
    if (!cached_tid) {
        cached_tid = syscall(SC_gettid);
        s_cached_tid = cached_tid;
    }
    return cached_tid;
}

int sysbeep()
{
    int rc = syscall(SC_beep);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fsync.html
int fsync(int fd)
{
    int rc = syscall(SC_fsync, fd);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int mount(int source_fd, const char* target, const char* fs_type, int flags)
{
    if (!target || !fs_type) {
        errno = EFAULT;
        return -1;
    }

    Syscall::SC_mount_params params {
        { target, strlen(target) },
        { fs_type, strlen(fs_type) },
        source_fd,
        flags
    };
    int rc = syscall(SC_mount, &params);
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

int set_process_name(const char* name, size_t name_length)
{
    int rc = syscall(SC_set_process_name, name, name_length);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int pledge(const char* promises, const char* execpromises)
{
    Syscall::SC_pledge_params params {
        { promises, promises ? strlen(promises) : 0 },
        { execpromises, execpromises ? strlen(execpromises) : 0 }
    };
    int rc = syscall(SC_pledge, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int unveil(const char* path, const char* permissions)
{
    Syscall::SC_unveil_params params {
        { path, path ? strlen(path) : 0 },
        { permissions, permissions ? strlen(permissions) : 0 }
    };
    int rc = syscall(SC_unveil, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getpass.html
char* getpass(const char* prompt)
{
    dbgln("FIXME: getpass('{}')", prompt);
    TODO();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sysconf.html
long sysconf(int name)
{
    int rc = syscall(SC_sysconf, name);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getpagesize.html
int getpagesize()
{
    return PAGE_SIZE;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/pause.html
int pause()
{
    return select(0, nullptr, nullptr, nullptr, nullptr);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/chroot.html
int chroot(const char* path)
{
    dbgln("FIXME: chroot(\"{}\")", path);
    return -1;
}
}
