/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/ScopedValueRollback.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <Kernel/API/Syscall.h>
#include <alloca.h>
#include <assert.h>
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
#include <sys/types.h>
#include <termios.h>
#include <time.h>
#include <unistd.h>

extern "C" {

static __thread int s_cached_tid = 0;
static int s_cached_pid = 0;

int chown(const char* pathname, uid_t uid, gid_t gid)
{
    if (!pathname) {
        errno = EFAULT;
        return -1;
    }
    Syscall::SC_chown_params params { { pathname, strlen(pathname) }, uid, gid };
    int rc = syscall(SC_chown, &params);
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
    if (rc == 0) {
        s_cached_tid = 0;
        s_cached_pid = 0;
    }
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
    if (strchr(filename, '/'))
        return execve(filename, argv, envp);

    ScopedValueRollback errno_rollback(errno);
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
    int saved_errno = errno;
    dbg() << "execvp() about to return " << rc << " with errno=" << saved_errno;
    errno = saved_errno;
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

uid_t geteuid()
{
    return syscall(SC_geteuid);
}

gid_t getegid()
{
    return syscall(SC_getegid);
}

uid_t getuid()
{
    return syscall(SC_getuid);
}

gid_t getgid()
{
    return syscall(SC_getgid);
}

pid_t getpid()
{
    int cached_pid = s_cached_pid;
    if (!cached_pid) {
        cached_pid = syscall(SC_getpid);
        s_cached_pid = cached_pid;
    }
    return cached_pid;
}

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

pid_t getsid(pid_t pid)
{
    int rc = syscall(SC_getsid, pid);
    __RETURN_WITH_ERRNO(rc, rc, -1);
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
    int rc = syscall(SC_ttyname, fd, buffer, size);
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
    struct timespec ts = { seconds, 0 };
    if (clock_nanosleep(CLOCK_MONOTONIC, 0, &ts, nullptr) < 0)
        return ts.tv_sec;
    return 0;
}

int usleep(useconds_t usec)
{
    struct timespec ts = { (long)(usec / 1000000), (long)(usec % 1000000) * 1000 };
    return clock_nanosleep(CLOCK_MONOTONIC, 0, &ts, nullptr);
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

ssize_t readlink(const char* path, char* buffer, size_t size)
{
    Syscall::SC_readlink_params params { { path, strlen(path) }, { buffer, size } };
    int rc = syscall(SC_readlink, &params);
    // Return the number of bytes placed in the buffer, not the full path size.
    __RETURN_WITH_ERRNO(rc, min((size_t)rc, size), -1);
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
    if (!target || !linkpath) {
        errno = EFAULT;
        return -1;
    }
    Syscall::SC_symlink_params params { { target, strlen(target) }, { linkpath, strlen(linkpath) } };
    int rc = syscall(SC_symlink, &params);
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
    return fcntl(fd, F_ISTTY);
}

int dup(int old_fd)
{
    return fcntl(old_fd, F_DUPFD, 0);
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
    if (!pathname) {
        errno = EFAULT;
        return -1;
    }
    Syscall::SC_mknod_params params { { pathname, strlen(pathname) }, mode, dev };
    int rc = syscall(SC_mknod, &params);
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

int set_process_icon(int icon_id)
{
    int rc = syscall(SC_set_process_icon, icon_id);
    __RETURN_WITH_ERRNO(rc, rc, -1);
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

int ftruncate(int fd, off_t length)
{
    int rc = syscall(SC_ftruncate, fd, length);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

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

int mount(int source_fd, const char* target, const char* fs_type, int flags)
{
    if (!target || !fs_type) {
        errno = EFAULT;
        return -1;
    }

    Syscall::SC_mount_params params {
        source_fd,
        { target, strlen(target) },
        { fs_type, strlen(fs_type) },
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

int chroot(const char* path)
{
    return chroot_with_mount_flags(path, -1);
}

int chroot_with_mount_flags(const char* path, int mount_flags)
{
    if (!path) {
        errno = EFAULT;
        return -1;
    }
    int rc = syscall(SC_chroot, path, strlen(path), mount_flags);
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

ssize_t pread(int fd, void* buf, size_t count, off_t offset)
{
    // FIXME: This is not thread safe and should be implemented in the kernel instead.
    off_t old_offset = lseek(fd, 0, SEEK_CUR);
    lseek(fd, offset, SEEK_SET);
    ssize_t nread = read(fd, buf, count);
    lseek(fd, old_offset, SEEK_SET);
    return nread;
}

char* getpass(const char* prompt)
{
    dbg() << "FIXME: getpass(\"" << prompt << "\")";
    ASSERT_NOT_REACHED();
}

long sysconf(int name)
{
    int rc = syscall(SC_sysconf, name);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int getpagesize()
{
    return PAGE_SIZE;
}
}
