/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/ScopeGuard.h>
#include <AK/ScopedValueRollback.h>
#include <AK/Vector.h>
#include <Kernel/API/Unveil.h>
#include <LibFileSystem/FileSystem.h>
#include <alloca.h>
#include <assert.h>
#include <bits/pthread_cancel.h>
#include <bits/pthread_integration.h>
#include <dirent.h>
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
#include <sys/prctl.h>
#include <sys/resource.h>
#include <sys/select.h>
#include <sys/stat.h>
#include <sys/sysmacros.h>
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
int lchown(char const* pathname, uid_t uid, gid_t gid)
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
int chown(char const* pathname, uid_t uid, gid_t gid)
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

int fchownat(int fd, char const* pathname, uid_t uid, gid_t gid, int flags)
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
int execv(char const* path, char* const argv[])
{
    return execve(path, argv, environ);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/execve.html
int execve(char const* filename, char* const argv[], char* const envp[])
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

// https://linux.die.net/man/3/execvpe (GNU extension)
int execvpe(char const* filename, char* const argv[], char* const envp[])
{
    if (strchr(filename, '/'))
        return execve(filename, argv, envp);

    ScopedValueRollback errno_rollback(errno);

    // TODO: Make this use the PATH search implementation from LibFileSystem.
    ByteString path = getenv("PATH");
    if (path.is_empty())
        path = DEFAULT_PATH;
    auto parts = path.split(':');
    for (auto& part : parts) {
        auto candidate = ByteString::formatted("{}/{}", part, filename);
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
int execvp(char const* filename, char* const argv[])
{
    int rc = execvpe(filename, argv, environ);
    int saved_errno = errno;
    dbgln("execvp({}, ...) about to return {} with errno={}", filename, rc, saved_errno);
    errno = saved_errno;
    return rc;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/execl.html
int execl(char const* filename, char const* arg0, ...)
{
    Vector<char const*, 16> args;
    args.append(arg0);

    va_list ap;
    va_start(ap, arg0);
    for (;;) {
        char const* arg = va_arg(ap, char const*);
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
int execlp(char const* filename, char const* arg0, ...)
{
    Vector<char const*, 16> args;
    args.append(arg0);

    va_list ap;
    va_start(ap, arg0);
    for (;;) {
        char const* arg = va_arg(ap, char const*);
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
    __pthread_maybe_cancel();

    int rc = syscall(SC_read, fd, buf, count);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/pread.html
ssize_t pread(int fd, void* buf, size_t count, off_t offset)
{
    __pthread_maybe_cancel();

    int rc = syscall(SC_pread, fd, buf, count, offset);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/write.html
ssize_t write(int fd, void const* buf, size_t count)
{
    __pthread_maybe_cancel();

    int rc = syscall(SC_write, fd, buf, count);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/pwrite.html
ssize_t pwrite(int fd, void const* buf, size_t count, off_t offset)
{
    __pthread_maybe_cancel();

    // FIXME: This is not thread safe and should be implemented in the kernel instead.
    off_t old_offset = lseek(fd, 0, SEEK_CUR);
    lseek(fd, offset, SEEK_SET);
    ssize_t nwritten = write(fd, buf, count);
    lseek(fd, old_offset, SEEK_SET);
    return nwritten;
}

// Note: Be sure to send to directory_name parameter a directory name ended with trailing slash.
static int ttyname_r_for_directory(char const* directory_name, dev_t device_mode, ino_t inode_number, char* buffer, size_t size)
{
    DIR* dirstream = opendir(directory_name);
    if (!dirstream) {
        return -1;
    }

    auto close_dir_stream_on_exit = ScopeGuard([dirstream] {
        closedir(dirstream);
    });

    struct dirent* entry = nullptr;
    char* name_path = nullptr;
    auto const directory_name_length = strlen(directory_name);

    // FIXME: Use LibCore DirIterator here instead
    while ((entry = readdir(dirstream)) != nullptr) {
        if (((ino_t)entry->d_ino == inode_number)
            && strcmp(entry->d_name, "stdin")
            && strcmp(entry->d_name, "stdout")
            && strcmp(entry->d_name, "stderr")) {

            size_t name_length = directory_name_length + strlen(entry->d_name) + 1;

            if (name_length > size) {
                errno = ERANGE;
                return -1;
            }

            name_path = (char*)malloc(name_length);
            // FIXME: ttyname_r() is not allowed to return ENOMEM, find better way to store name_path,
            //        perhaps a static storage.
            VERIFY(name_path);
            memset(name_path, 0, name_length);
            memcpy(name_path, directory_name, directory_name_length);
            memcpy(&name_path[directory_name_length], entry->d_name, strlen(entry->d_name));
            struct stat st;
            if (lstat(name_path, &st) < 0) {
                free(name_path);
                name_path = nullptr;
                continue;
            }

            if (device_mode == st.st_rdev) {
                memset(buffer, 0, name_length);
                memcpy(buffer, name_path, name_length);
                free(name_path);
                return 0;
            }
        }
    }
    free(name_path);
    return -1;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/ttyname_r.html
int ttyname_r(int fd, char* buffer, size_t size)
{
    struct stat stat;
    if (fstat(fd, &stat) < 0)
        return -1;
    dev_t major_minor_numbers = stat.st_rdev;
    ino_t inode_number = stat.st_ino;
    if (ttyname_r_for_directory("/dev/", major_minor_numbers, inode_number, buffer, size) < 0) {
        if (ttyname_r_for_directory("/dev/pts/", major_minor_numbers, inode_number, buffer, size) < 0) {
            errno = ENOTTY;
            return -1;
        }
    }
    return 0;
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
    __pthread_maybe_cancel();

    int rc = syscall(SC_close, fd);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/chdir.html
int chdir(char const* path)
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

int sethostname(char const* hostname, ssize_t size)
{
    int rc = syscall(SC_sethostname, hostname, size);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/readlinkat.html
ssize_t readlink(char const* path, char* buffer, size_t size)
{
    return readlinkat(AT_FDCWD, path, buffer, size);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/readlinkat.html
ssize_t readlinkat(int dirfd, char const* path, char* buffer, size_t size)
{
    Syscall::SC_readlink_params params { { path, strlen(path) }, { buffer, size }, dirfd };
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
int link(char const* old_path, char const* new_path)
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
int unlink(char const* pathname)
{
    return unlinkat(AT_FDCWD, pathname, 0);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/unlinkat.html
int unlinkat(int dirfd, char const* pathname, int flags)
{
    int rc = syscall(SC_unlink, dirfd, pathname, strlen(pathname), flags);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/symlink.html
int symlink(char const* target, char const* linkpath)
{
    return symlinkat(target, AT_FDCWD, linkpath);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/symlinkat.html
int symlinkat(char const* target, int newdirfd, char const* linkpath)
{
    if (!target || !linkpath) {
        errno = EFAULT;
        return -1;
    }
    Syscall::SC_symlink_params params { { target, strlen(target) }, { linkpath, strlen(linkpath) }, newdirfd };
    int rc = syscall(SC_symlink, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/rmdir.html
int rmdir(char const* pathname)
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

int setgroups(size_t size, gid_t const* list)
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

int setregid(gid_t rgid, gid_t egid)
{
    int rc = syscall(SC_setresgid, rgid, egid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int setresgid(gid_t rgid, gid_t egid, gid_t sgid)
{
    int rc = syscall(SC_setresgid, rgid, egid, sgid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/access.html
int access(char const* pathname, int mode)
{
    return faccessat(AT_FDCWD, pathname, mode, 0);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/faccessat.html
int faccessat(int dirfd, char const* pathname, int mode, int flags)
{
    if (!pathname) {
        errno = EFAULT;
        return -1;
    }

    Syscall::SC_faccessat_params params { dirfd, { pathname, strlen(pathname) }, mode, flags };
    int rc = syscall(SC_faccessat, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mknod.html
int mknod(char const* pathname, mode_t mode, dev_t dev)
{
    return mknodat(AT_FDCWD, pathname, mode, dev);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mknodat.html
int mknodat(int dirfd, char const* pathname, mode_t mode, dev_t dev)
{
    if (!pathname) {
        errno = EFAULT;
        return -1;
    }
    Syscall::SC_mknod_params params { { pathname, strlen(pathname) }, mode, dev, dirfd };
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
long pathconf([[maybe_unused]] char const* path, int name)
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

static Optional<ByteString> getlogin_buffer {};

char* getlogin()
{
    if (!getlogin_buffer.has_value()) {
        if (auto* passwd = getpwuid(getuid())) {
            getlogin_buffer = ByteString(passwd->pw_name);
        }
        endpwent();
    }
    return const_cast<char*>(getlogin_buffer->characters());
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/ftruncate.html
int ftruncate(int fd, off_t length)
{
    int rc = syscall(SC_ftruncate, fd, length);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/truncate.html
int truncate(char const* path, off_t length)
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fsync.html
int fsync(int fd)
{
    __pthread_maybe_cancel();

    int rc = syscall(SC_fsync, fd);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int fsopen(char const* fs_type, int flags)
{
    if (!fs_type) {
        errno = EFAULT;
        return -1;
    }

    Syscall::SC_fsopen_params params {
        { fs_type, strlen(fs_type) },
        flags,
    };
    int rc = syscall(SC_fsopen, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int fsmount(int vfs_context_id, int mount_fd, int source_fd, char const* target)
{
    if (!target) {
        errno = EFAULT;
        return -1;
    }

    Syscall::SC_fsmount_params params {
        vfs_context_id,
        mount_fd,
        { target, strlen(target) },
        source_fd,
    };
    int rc = syscall(SC_fsmount, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int bindmount(int vfs_context_id, int source_fd, char const* target, int flags)
{
    if (!target) {
        errno = EFAULT;
        return -1;
    }

    Syscall::SC_bindmount_params params {
        vfs_context_id,
        { target, strlen(target) },
        source_fd,
        flags,
    };
    int rc = syscall(SC_bindmount, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int mount(int source_fd, char const* target, char const* fs_type, int flags)
{
    if (flags & MS_BIND)
        return bindmount(-1, source_fd, target, flags);

    int mount_fd = fsopen(fs_type, flags);
    if (mount_fd < 0)
        return -1;

    return fsmount(-1, mount_fd, source_fd, target);
}

int umount(char const* mountpoint)
{
    Syscall::SC_umount_params params {
        -1,
        { mountpoint, strlen(mountpoint) },
    };
    int rc = syscall(SC_umount, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

void dump_backtrace()
{
    syscall(SC_dump_backtrace);
}

int get_process_name(char* buffer, int buffer_size)
{
    int rc = syscall(SC_prctl, PR_GET_PROCESS_NAME, buffer, buffer_size, nullptr);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int set_process_name(char const* name, size_t name_length)
{
    int rc = syscall(SC_prctl, PR_SET_PROCESS_NAME, name, name_length, nullptr);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int pledge(char const* promises, char const* execpromises)
{
    Syscall::SC_pledge_params params {
        { promises, promises ? strlen(promises) : 0 },
        { execpromises, execpromises ? strlen(execpromises) : 0 }
    };
    int rc = syscall(SC_pledge, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int unveil(char const* path, char const* permissions)
{
    Syscall::SC_unveil_params params {
        static_cast<int>(UnveilFlags::CurrentProgram),
        { path, path ? strlen(path) : 0 },
        { permissions, permissions ? strlen(permissions) : 0 }
    };
    int rc = syscall(SC_unveil, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/7908799/xsh/getpass.html
char* getpass(char const* prompt)
{
    int tty = open("/dev/tty", O_RDWR | O_NOCTTY | O_CLOEXEC);
    if (tty < 0)
        return nullptr;

    static char password[PASS_MAX];
    ssize_t chars_read;

    {
        auto close_tty = ScopeGuard([tty] {
            close(tty);
        });

        struct termios backup { };
        if (tcgetattr(tty, &backup) < 0)
            return nullptr;

        {
            auto restore_termios = ScopeGuard([tty, backup] {
                tcsetattr(tty, TCSAFLUSH, &backup);
            });

            struct termios noecho = backup;
            noecho.c_lflag &= ~(ECHO);
            noecho.c_lflag |= ICANON;

            if (tcsetattr(tty, TCSAFLUSH, &noecho) < 0)
                return nullptr;
            if (tcdrain(tty) < 0)
                return nullptr;

            if (prompt) {
                if (write(tty, prompt, strlen(prompt)) < 0)
                    return nullptr;
            }

            chars_read = read(tty, password, sizeof(password));
        }

        write(tty, "\n", 1);
    }

    if (chars_read < 0)
        return nullptr;

    if (chars_read > 0 && (password[chars_read - 1] == '\n' || chars_read == sizeof(password)))
        password[chars_read - 1] = '\0';
    else
        password[chars_read] = '\0';

    return password;
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
int chroot(char const* path)
{
    dbgln("FIXME: chroot(\"{}\")", path);
    return -1;
}

// https://pubs.opengroup.org/onlinepubs/7908799/xsh/getdtablesize.html
int getdtablesize()
{
    rlimit dtablesize;
    int rc = getrlimit(RLIMIT_NOFILE, &dtablesize);
    __RETURN_WITH_ERRNO(rc, dtablesize.rlim_cur, rc);
}

// https://pubs.opengroup.org/onlinepubs/007904975/functions/nice.html
int nice(int incr)
{
    dbgln("FIXME: nice was called with: {}, not implemented", incr);
    return incr;
}

int brk(void* addr)
{
    dbgln("TODO: brk({:#x})", addr);
    errno = ENOMEM;
    return -1;
}

void* sbrk(intptr_t incr)
{
    dbgln("TODO: sbrk({:#x})", incr);
    errno = ENOMEM;
    return reinterpret_cast<void*>(-1);
}
}
