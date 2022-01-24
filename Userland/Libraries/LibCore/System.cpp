/*
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Kenneth Myhra <kennethmyhra@gmail.com>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Matthias Zimmerman <matthias291999@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/System.h>
#include <LibSystem/syscall.h>
#include <limits.h>
#include <stdarg.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/ptrace.h>
#include <sys/time.h>
#include <termios.h>
#include <unistd.h>

#ifdef __serenity__
#    include <serenity.h>
#endif

#define HANDLE_SYSCALL_RETURN_VALUE(syscall_name, rc, success_value) \
    if ((rc) < 0) {                                                  \
        return Error::from_syscall(syscall_name, rc);                \
    }                                                                \
    return success_value;

namespace Core::System {

#ifndef HOST_NAME_MAX
#    ifdef __APPLE__
#        define HOST_NAME_MAX 255
#    else
#        define HOST_NAME_MAX 64
#    endif
#endif

#ifdef __serenity__

ErrorOr<void> beep()
{
    auto rc = ::sysbeep();
    if (rc < 0)
        return Error::from_syscall("beep"sv, -errno);
    return {};
}

ErrorOr<void> pledge(StringView promises, StringView execpromises)
{
    Syscall::SC_pledge_params params {
        { promises.characters_without_null_termination(), promises.length() },
        { execpromises.characters_without_null_termination(), execpromises.length() },
    };
    int rc = syscall(SC_pledge, &params);
    HANDLE_SYSCALL_RETURN_VALUE("pledge"sv, rc, {});
}

ErrorOr<void> unveil(StringView path, StringView permissions)
{
    Syscall::SC_unveil_params params {
        { path.characters_without_null_termination(), path.length() },
        { permissions.characters_without_null_termination(), permissions.length() },
    };
    int rc = syscall(SC_unveil, &params);
    HANDLE_SYSCALL_RETURN_VALUE("unveil"sv, rc, {});
}

ErrorOr<void> sendfd(int sockfd, int fd)
{
    if (::sendfd(sockfd, fd) < 0)
        return Error::from_syscall("sendfd"sv, -errno);
    return {};
}

ErrorOr<int> recvfd(int sockfd, int options)
{
    auto fd = ::recvfd(sockfd, options);
    if (fd < 0)
        return Error::from_syscall("recvfd"sv, -errno);
    return fd;
}

ErrorOr<void> ptrace_peekbuf(pid_t tid, void const* tracee_addr, Bytes destination_buf)
{
    Syscall::SC_ptrace_buf_params buf_params {
        { destination_buf.data(), destination_buf.size() }
    };
    Syscall::SC_ptrace_params params {
        PT_PEEKBUF,
        tid,
        const_cast<void*>(tracee_addr),
        (FlatPtr)&buf_params,
    };
    int rc = syscall(SC_ptrace, &params);
    HANDLE_SYSCALL_RETURN_VALUE("ptrace_peekbuf", rc, {});
}

ErrorOr<void> setgroups(Span<gid_t const> gids)
{
    if (::setgroups(gids.size(), gids.data()) < 0)
        return Error::from_syscall("setgroups"sv, -errno);
    return {};
}

ErrorOr<void> mount(int source_fd, StringView target, StringView fs_type, int flags)
{
    if (target.is_null() || fs_type.is_null())
        return Error::from_errno(EFAULT);

    Syscall::SC_mount_params params {
        { target.characters_without_null_termination(), target.length() },
        { fs_type.characters_without_null_termination(), fs_type.length() },
        source_fd,
        flags
    };
    int rc = syscall(SC_mount, &params);
    HANDLE_SYSCALL_RETURN_VALUE("mount", rc, {});
}

ErrorOr<void> umount(StringView mount_point)
{
    if (mount_point.is_null())
        return Error::from_errno(EFAULT);

    int rc = syscall(SC_umount, mount_point.characters_without_null_termination(), mount_point.length());
    HANDLE_SYSCALL_RETURN_VALUE("umount", rc, {});
}

ErrorOr<long> ptrace(int request, pid_t tid, void* address, void* data)
{
    auto rc = ::ptrace(request, tid, address, data);
    if (rc < 0)
        return Error::from_syscall("ptrace"sv, -errno);
    return rc;
}

ErrorOr<void> disown(pid_t pid)
{
    int rc = ::disown(pid);
    HANDLE_SYSCALL_RETURN_VALUE("disown", rc, {});
}
#endif

#ifndef AK_OS_BSD_GENERIC
ErrorOr<Optional<struct spwd>> getspent()
{
    errno = 0;
    if (auto* spwd = ::getspent())
        return *spwd;
    if (errno)
        return Error::from_syscall("getspent"sv, -errno);
    return Optional<struct spwd> {};
}

ErrorOr<Optional<struct spwd>> getspnam(StringView name)
{
    errno = 0;
    ::setspent();
    while (auto* spwd = ::getspent()) {
        if (spwd->sp_namp == name)
            return *spwd;
    }
    if (errno)
        return Error::from_syscall("getspnam"sv, -errno);
    return Optional<struct spwd> {};
}
#endif

#ifndef AK_OS_MACOS
ErrorOr<int> accept4(int sockfd, sockaddr* address, socklen_t* address_length, int flags)
{
    auto fd = ::accept4(sockfd, address, address_length, flags);
    if (fd < 0)
        return Error::from_syscall("accept4"sv, -errno);
    return fd;
}
#endif

ErrorOr<void> sigaction(int signal, struct sigaction const* action, struct sigaction* old_action)
{
    if (::sigaction(signal, action, old_action) < 0)
        return Error::from_syscall("sigaction"sv, -errno);
    return {};
}

#if defined(__APPLE__) || defined(__OpenBSD__)
ErrorOr<sig_t> signal(int signal, sig_t handler)
#else
ErrorOr<sighandler_t> signal(int signal, sighandler_t handler)
#endif
{
    auto old_handler = ::signal(signal, handler);
    if (old_handler == SIG_ERR)
        return Error::from_syscall("signal"sv, -errno);
    return old_handler;
}

ErrorOr<struct stat> fstat(int fd)
{
    struct stat st = {};
    if (::fstat(fd, &st) < 0)
        return Error::from_syscall("fstat"sv, -errno);
    return st;
}

ErrorOr<int> fcntl(int fd, int command, ...)
{
    va_list ap;
    va_start(ap, command);
    u32 extra_arg = va_arg(ap, u32);
    int rc = ::fcntl(fd, command, extra_arg);
    va_end(ap);
    if (rc < 0)
        return Error::from_syscall("fcntl"sv, -errno);
    return rc;
}

ErrorOr<void*> mmap(void* address, size_t size, int protection, int flags, int fd, off_t offset, [[maybe_unused]] size_t alignment, [[maybe_unused]] StringView name)
{
#ifdef __serenity__
    Syscall::SC_mmap_params params { address, size, alignment, protection, flags, fd, offset, { name.characters_without_null_termination(), name.length() } };
    ptrdiff_t rc = syscall(SC_mmap, &params);
    if (rc < 0 && rc > -EMAXERRNO)
        return Error::from_syscall("mmap"sv, rc);
    return reinterpret_cast<void*>(rc);
#else
    // NOTE: Regular POSIX mmap() doesn't support custom alignment requests.
    VERIFY(!alignment);
    auto* ptr = ::mmap(address, size, protection, flags, fd, offset);
    if (ptr == MAP_FAILED)
        return Error::from_syscall("mmap"sv, -errno);
    return ptr;
#endif
}

ErrorOr<void> munmap(void* address, size_t size)
{
    if (::munmap(address, size) < 0)
        return Error::from_syscall("munmap"sv, -errno);
    return {};
}

ErrorOr<int> open(StringView path, int options, ...)
{
    if (!path.characters_without_null_termination())
        return Error::from_syscall("open"sv, -EFAULT);
    va_list ap;
    va_start(ap, options);
    auto mode = (mode_t)va_arg(ap, unsigned);
    va_end(ap);
#ifdef __serenity__
    Syscall::SC_open_params params { AT_FDCWD, { path.characters_without_null_termination(), path.length() }, options, mode };
    int rc = syscall(SC_open, &params);
    HANDLE_SYSCALL_RETURN_VALUE("open"sv, rc, rc);
#else
    // NOTE: We have to ensure that the path is null-terminated.
    String path_string = path;
    int rc = ::open(path_string.characters(), options, mode);
    if (rc < 0)
        return Error::from_syscall("open"sv, -errno);
    return rc;
#endif
}

ErrorOr<void> close(int fd)
{
    if (::close(fd) < 0)
        return Error::from_syscall("close"sv, -errno);
    return {};
}

ErrorOr<void> ftruncate(int fd, off_t length)
{
    if (::ftruncate(fd, length) < 0)
        return Error::from_syscall("ftruncate"sv, -errno);
    return {};
}

ErrorOr<struct stat> stat(StringView path)
{
    if (!path.characters_without_null_termination())
        return Error::from_syscall("stat"sv, -EFAULT);

    struct stat st = {};
#ifdef __serenity__
    Syscall::SC_stat_params params { { path.characters_without_null_termination(), path.length() }, &st, AT_FDCWD, true };
    int rc = syscall(SC_stat, &params);
    HANDLE_SYSCALL_RETURN_VALUE("stat"sv, rc, st);
#else
    String path_string = path;
    if (::stat(path_string.characters(), &st) < 0)
        return Error::from_syscall("stat"sv, -errno);
    return st;
#endif
}

ErrorOr<struct stat> lstat(StringView path)
{
    if (!path.characters_without_null_termination())
        return Error::from_syscall("lstat"sv, -EFAULT);

    struct stat st = {};
#ifdef __serenity__
    Syscall::SC_stat_params params { { path.characters_without_null_termination(), path.length() }, &st, AT_FDCWD, false };
    int rc = syscall(SC_stat, &params);
    HANDLE_SYSCALL_RETURN_VALUE("lstat"sv, rc, st);
#else
    String path_string = path;
    if (::stat(path_string.characters(), &st) < 0)
        return Error::from_syscall("lstat"sv, -errno);
    return st;
#endif
}

ErrorOr<ssize_t> read(int fd, Bytes buffer)
{
    ssize_t rc = ::read(fd, buffer.data(), buffer.size());
    if (rc < 0)
        return Error::from_syscall("read"sv, -errno);
    return rc;
}

ErrorOr<ssize_t> write(int fd, ReadonlyBytes buffer)
{
    ssize_t rc = ::write(fd, buffer.data(), buffer.size());
    if (rc < 0)
        return Error::from_syscall("write"sv, -errno);
    return rc;
}

ErrorOr<void> kill(pid_t pid, int signal)
{
    if (::kill(pid, signal) < 0)
        return Error::from_syscall("kill"sv, -errno);
    return {};
}

ErrorOr<int> dup(int source_fd)
{
    int fd = ::dup(source_fd);
    if (fd < 0)
        return Error::from_syscall("dup"sv, -errno);
    return fd;
}

ErrorOr<int> dup2(int source_fd, int destination_fd)
{
    int fd = ::dup2(source_fd, destination_fd);
    if (fd < 0)
        return Error::from_syscall("dup2"sv, -errno);
    return fd;
}

ErrorOr<String> ptsname(int fd)
{
    auto* name = ::ptsname(fd);
    if (!name)
        return Error::from_syscall("ptsname"sv, -errno);
    return String(name);
}

ErrorOr<String> gethostname()
{
    char hostname[HOST_NAME_MAX];
    int rc = ::gethostname(hostname, sizeof(hostname));
    if (rc < 0)
        return Error::from_syscall("gethostname"sv, -errno);
    return String(&hostname[0]);
}

ErrorOr<void> sethostname(StringView hostname)
{
    int rc = ::sethostname(hostname.characters_without_null_termination(), hostname.length());
    if (rc < 0)
        return Error::from_syscall("sethostname"sv, -errno);
    return {};
}

ErrorOr<String> getcwd()
{
    auto* cwd = ::getcwd(nullptr, 0);
    if (!cwd)
        return Error::from_syscall("getcwd"sv, -errno);

    String string_cwd(cwd);
    free(cwd);
    return string_cwd;
}

ErrorOr<void> ioctl(int fd, unsigned request, ...)
{
    va_list ap;
    va_start(ap, request);
    FlatPtr arg = va_arg(ap, FlatPtr);
    va_end(ap);
    if (::ioctl(fd, request, arg) < 0)
        return Error::from_syscall("ioctl"sv, -errno);
    return {};
}

ErrorOr<struct termios> tcgetattr(int fd)
{
    struct termios ios = {};
    if (::tcgetattr(fd, &ios) < 0)
        return Error::from_syscall("tcgetattr"sv, -errno);
    return ios;
}

ErrorOr<void> tcsetattr(int fd, int optional_actions, struct termios const& ios)
{
    if (::tcsetattr(fd, optional_actions, &ios) < 0)
        return Error::from_syscall("tcsetattr"sv, -errno);
    return {};
}

ErrorOr<void> chmod(StringView pathname, mode_t mode)
{
    if (!pathname.characters_without_null_termination())
        return Error::from_syscall("chmod"sv, -EFAULT);

#ifdef __serenity__
    Syscall::SC_chmod_params params {
        AT_FDCWD,
        { pathname.characters_without_null_termination(), pathname.length() },
        mode,
        true
    };
    int rc = syscall(SC_chmod, &params);
    HANDLE_SYSCALL_RETURN_VALUE("chmod"sv, rc, {});
#else
    String path = pathname;
    if (::chmod(path.characters(), mode) < 0)
        return Error::from_syscall("chmod"sv, -errno);
    return {};
#endif
}

ErrorOr<void> fchmod(int fd, mode_t mode)
{
    if (::fchmod(fd, mode) < 0)
        return Error::from_syscall("fchmod"sv, -errno);
    return {};
}

ErrorOr<void> lchown(StringView pathname, uid_t uid, gid_t gid)
{
    if (!pathname.characters_without_null_termination())
        return Error::from_syscall("chown"sv, -EFAULT);

#ifdef __serenity__
    Syscall::SC_chown_params params = { { pathname.characters_without_null_termination(), pathname.length() }, uid, gid, AT_FDCWD, false };
    int rc = syscall(SC_chown, &params);
    HANDLE_SYSCALL_RETURN_VALUE("chown"sv, rc, {});
#else
    String path = pathname;
    if (::chown(path.characters(), uid, gid) < 0)
        return Error::from_syscall("chown"sv, -errno);
    return {};
#endif
}

ErrorOr<void> chown(StringView pathname, uid_t uid, gid_t gid)
{
    if (!pathname.characters_without_null_termination())
        return Error::from_syscall("chown"sv, -EFAULT);

#ifdef __serenity__
    Syscall::SC_chown_params params = { { pathname.characters_without_null_termination(), pathname.length() }, uid, gid, AT_FDCWD, true };
    int rc = syscall(SC_chown, &params);
    HANDLE_SYSCALL_RETURN_VALUE("chown"sv, rc, {});
#else
    String path = pathname;
    if (::lchown(path.characters(), uid, gid) < 0)
        return Error::from_syscall("lchown"sv, -errno);
    return {};
#endif
}

ErrorOr<Optional<struct passwd>> getpwuid(uid_t uid)
{
    errno = 0;
    if (auto* pwd = ::getpwuid(uid))
        return *pwd;
    if (errno)
        return Error::from_syscall("getpwuid"sv, -errno);
    return Optional<struct passwd> {};
}

ErrorOr<Optional<struct group>> getgrgid(gid_t gid)
{
    errno = 0;
    if (auto* grp = ::getgrgid(gid))
        return *grp;
    if (errno)
        return Error::from_syscall("getgrgid"sv, -errno);
    return Optional<struct group> {};
}

ErrorOr<Optional<struct passwd>> getpwnam(StringView name)
{
    errno = 0;

    ::setpwent();
    if (errno)
        return Error::from_syscall("getpwnam"sv, -errno);

    while (auto* pw = ::getpwent()) {
        if (errno)
            return Error::from_syscall("getpwnam"sv, -errno);
        if (pw->pw_name == name)
            return *pw;
    }
    if (errno)
        return Error::from_syscall("getpwnam"sv, -errno);
    else
        return Optional<struct passwd> {};
}

ErrorOr<Optional<struct group>> getgrnam(StringView name)
{
    errno = 0;

    ::setgrent();
    if (errno)
        return Error::from_syscall("getgrnam"sv, -errno);

    while (auto* gr = ::getgrent()) {
        if (errno)
            return Error::from_syscall("getgrnam"sv, -errno);
        if (gr->gr_name == name)
            return *gr;
    }
    if (errno)
        return Error::from_syscall("getgrnam"sv, -errno);
    else
        return Optional<struct group> {};
}

ErrorOr<void> clock_settime(clockid_t clock_id, struct timespec* ts)
{
#ifdef __serenity__
    int rc = syscall(SC_clock_settime, clock_id, ts);
    HANDLE_SYSCALL_RETURN_VALUE("clocksettime"sv, rc, {});
#else
    if (::clock_settime(clock_id, ts) < 0)
        return Error::from_syscall("clocksettime"sv, -errno);
    return {};
#endif
}

ErrorOr<pid_t> posix_spawnp(StringView const path, posix_spawn_file_actions_t* const file_actions, posix_spawnattr_t* const attr, char* const arguments[], char* const envp[])
{
    pid_t child_pid;
    if ((errno = ::posix_spawnp(&child_pid, path.to_string().characters(), file_actions, attr, arguments, envp)))
        return Error::from_syscall("posix_spawnp"sv, -errno);
    return child_pid;
}

ErrorOr<off_t> lseek(int fd, off_t offset, int whence)
{
    off_t rc = ::lseek(fd, offset, whence);
    if (rc < 0)
        return Error::from_syscall("lseek", -errno);
    return rc;
}

ErrorOr<WaitPidResult> waitpid(pid_t waitee, int options)
{
    int wstatus;
    pid_t pid = ::waitpid(waitee, &wstatus, options);
    if (pid < 0)
        return Error::from_syscall("waitpid"sv, -errno);
    return WaitPidResult { pid, wstatus };
}

ErrorOr<void> setuid(uid_t uid)
{
    if (::setuid(uid) < 0)
        return Error::from_syscall("setuid"sv, -errno);
    return {};
}

ErrorOr<void> seteuid(uid_t uid)
{
    if (::seteuid(uid) < 0)
        return Error::from_syscall("seteuid"sv, -errno);
    return {};
}

ErrorOr<void> setgid(gid_t gid)
{
    if (::setgid(gid) < 0)
        return Error::from_syscall("setgid"sv, -errno);
    return {};
}

ErrorOr<void> setegid(gid_t gid)
{
    if (::setegid(gid) < 0)
        return Error::from_syscall("setegid"sv, -errno);
    return {};
}

ErrorOr<void> setpgid(pid_t pid, pid_t pgid)
{
    if (::setpgid(pid, pgid) < 0)
        return Error::from_syscall("setpgid"sv, -errno);
    return {};
}

ErrorOr<pid_t> setsid()
{
    int rc = ::setsid();
    if (rc < 0)
        return Error::from_syscall("setsid"sv, -errno);
    return rc;
}

ErrorOr<bool> isatty(int fd)
{
    int rc = ::isatty(fd);
    if (rc < 0)
        return Error::from_syscall("isatty"sv, -errno);
    return rc == 1;
}

ErrorOr<void> symlink(StringView target, StringView link_path)
{
#ifdef __serenity__
    Syscall::SC_symlink_params params {
        .target = { target.characters_without_null_termination(), target.length() },
        .linkpath = { link_path.characters_without_null_termination(), link_path.length() },
    };
    int rc = syscall(SC_symlink, &params);
    HANDLE_SYSCALL_RETURN_VALUE("symlink"sv, rc, {});
#else
    String target_string = target;
    String link_path_string = link_path;
    if (::symlink(target_string.characters(), link_path_string.characters()) < 0)
        return Error::from_syscall("symlink"sv, -errno);
    return {};
#endif
}

ErrorOr<void> mkdir(StringView path, mode_t mode)
{
    if (path.is_null())
        return Error::from_errno(EFAULT);
#ifdef __serenity__
    int rc = syscall(SC_mkdir, path.characters_without_null_termination(), path.length(), mode);
    HANDLE_SYSCALL_RETURN_VALUE("mkdir"sv, rc, {});
#else
    String path_string = path;
    if (::mkdir(path_string.characters(), mode) < 0)
        return Error::from_syscall("mkdir"sv, -errno);
    return {};
#endif
}

ErrorOr<void> chdir(StringView path)
{
    if (path.is_null())
        return Error::from_errno(EFAULT);
#ifdef __serenity__
    int rc = syscall(SC_chdir, path.characters_without_null_termination(), path.length());
    HANDLE_SYSCALL_RETURN_VALUE("chdir"sv, rc, {});
#else
    String path_string = path;
    if (::chdir(path_string.characters()) < 0)
        return Error::from_syscall("chdir"sv, -errno);
    return {};
#endif
}

ErrorOr<pid_t> fork()
{
    pid_t pid = ::fork();
    if (pid < 0)
        return Error::from_syscall("fork"sv, -errno);
    return pid;
}

ErrorOr<int> mkstemp(Span<char> pattern)
{
    int fd = ::mkstemp(pattern.data());
    if (fd < 0)
        return Error::from_syscall("mkstemp"sv, -errno);
    return fd;
}

ErrorOr<void> rename(StringView old_path, StringView new_path)
{
    if (old_path.is_null() || new_path.is_null())
        return Error::from_errno(EFAULT);

#ifdef __serenity__
    Syscall::SC_rename_params params {
        .old_path = { old_path.characters_without_null_termination(), old_path.length() },
        .new_path = { new_path.characters_without_null_termination(), new_path.length() },
    };
    int rc = syscall(SC_rename, &params);
    HANDLE_SYSCALL_RETURN_VALUE("rename"sv, rc, {});
#else
    String old_path_string = old_path;
    String new_path_string = new_path;
    if (::rename(old_path_string.characters(), new_path_string.characters()) < 0)
        return Error::from_syscall("rename"sv, -errno);
    return {};
#endif
}

ErrorOr<void> unlink(StringView path)
{
    if (path.is_null())
        return Error::from_errno(EFAULT);

#ifdef __serenity__
    int rc = syscall(SC_unlink, path.characters_without_null_termination(), path.length());
    HANDLE_SYSCALL_RETURN_VALUE("unlink"sv, rc, {});
#else
    String path_string = path;
    if (::unlink(path_string.characters()) < 0)
        return Error::from_syscall("unlink"sv, -errno);
    return {};
#endif
}

ErrorOr<void> utime(StringView path, Optional<struct utimbuf> maybe_buf)
{
    if (path.is_null())
        return Error::from_errno(EFAULT);

    struct utimbuf* buf = nullptr;
    if (maybe_buf.has_value())
        buf = &maybe_buf.value();
#ifdef __serenity__
    int rc = syscall(SC_utime, path.characters_without_null_termination(), path.length(), buf);
    HANDLE_SYSCALL_RETURN_VALUE("utime"sv, rc, {});
#else
    String path_string = path;
    if (::utime(path_string.characters(), buf) < 0)
        return Error::from_syscall("utime"sv, -errno);
    return {};
#endif
}

ErrorOr<struct utsname> uname()
{
    utsname uts;
#ifdef __serenity__
    int rc = syscall(SC_uname, &uts);
    HANDLE_SYSCALL_RETURN_VALUE("uname"sv, rc, uts);
#else
    if (::uname(&uts) < 0)
        return Error::from_syscall("uname"sv, -errno);
#endif
    return uts;
}

ErrorOr<void> adjtime(const struct timeval* delta, struct timeval* old_delta)
{
#ifdef __serenity__
    int rc = syscall(SC_adjtime, delta, old_delta);
    HANDLE_SYSCALL_RETURN_VALUE("adjtime"sv, rc, {});
#else
    if (::adjtime(delta, old_delta) < 0)
        return Error::from_syscall("adjtime"sv, -errno);
    return {};
#endif
}

ErrorOr<int> socket(int domain, int type, int protocol)
{
    auto fd = ::socket(domain, type, protocol);
    if (fd < 0)
        return Error::from_syscall("socket"sv, -errno);
    return fd;
}

ErrorOr<void> bind(int sockfd, struct sockaddr const* address, socklen_t address_length)
{
    if (::bind(sockfd, address, address_length) < 0)
        return Error::from_syscall("bind"sv, -errno);
    return {};
}

ErrorOr<void> listen(int sockfd, int backlog)
{
    if (::listen(sockfd, backlog) < 0)
        return Error::from_syscall("listen"sv, -errno);
    return {};
}

ErrorOr<int> accept(int sockfd, struct sockaddr* address, socklen_t* address_length)
{
    auto fd = ::accept(sockfd, address, address_length);
    if (fd < 0)
        return Error::from_syscall("accept"sv, -errno);
    return fd;
}

ErrorOr<void> connect(int sockfd, struct sockaddr const* address, socklen_t address_length)
{
    if (::connect(sockfd, address, address_length) < 0)
        return Error::from_syscall("connect"sv, -errno);
    return {};
}

ErrorOr<void> shutdown(int sockfd, int how)
{
    if (::shutdown(sockfd, how) < 0)
        return Error::from_syscall("shutdown"sv, -errno);
    return {};
}

ErrorOr<ssize_t> send(int sockfd, void const* buffer, size_t buffer_length, int flags)
{
    auto sent = ::send(sockfd, buffer, buffer_length, flags);
    if (sent < 0)
        return Error::from_syscall("send"sv, -errno);
    return sent;
}

ErrorOr<ssize_t> sendmsg(int sockfd, const struct msghdr* message, int flags)
{
    auto sent = ::sendmsg(sockfd, message, flags);
    if (sent < 0)
        return Error::from_syscall("sendmsg"sv, -errno);
    return sent;
}

ErrorOr<ssize_t> sendto(int sockfd, void const* source, size_t source_length, int flags, struct sockaddr const* destination, socklen_t destination_length)
{
    auto sent = ::sendto(sockfd, source, source_length, flags, destination, destination_length);
    if (sent < 0)
        return Error::from_syscall("sendto"sv, -errno);
    return sent;
}

ErrorOr<ssize_t> recv(int sockfd, void* buffer, size_t length, int flags)
{
    auto received = ::recv(sockfd, buffer, length, flags);
    if (received < 0)
        return Error::from_syscall("recv"sv, -errno);
    return received;
}

ErrorOr<ssize_t> recvmsg(int sockfd, struct msghdr* message, int flags)
{
    auto received = ::recvmsg(sockfd, message, flags);
    if (received < 0)
        return Error::from_syscall("recvmsg"sv, -errno);
    return received;
}

ErrorOr<ssize_t> recvfrom(int sockfd, void* buffer, size_t buffer_length, int flags, struct sockaddr* address, socklen_t* address_length)
{
    auto received = ::recvfrom(sockfd, buffer, buffer_length, flags, address, address_length);
    if (received < 0)
        return Error::from_syscall("recvfrom"sv, -errno);
    return received;
}

ErrorOr<void> getsockopt(int sockfd, int level, int option, void* value, socklen_t* value_size)
{
    if (::getsockopt(sockfd, level, option, value, value_size) < 0)
        return Error::from_syscall("getsockopt"sv, -errno);
    return {};
}

ErrorOr<void> setsockopt(int sockfd, int level, int option, void const* value, socklen_t value_size)
{
    if (::setsockopt(sockfd, level, option, value, value_size) < 0)
        return Error::from_syscall("setsockopt"sv, -errno);
    return {};
}

ErrorOr<void> getsockname(int sockfd, struct sockaddr* address, socklen_t* address_length)
{
    if (::getsockname(sockfd, address, address_length) < 0)
        return Error::from_syscall("getsockname"sv, -errno);
    return {};
}

ErrorOr<void> getpeername(int sockfd, struct sockaddr* address, socklen_t* address_length)
{
    if (::getpeername(sockfd, address, address_length) < 0)
        return Error::from_syscall("getpeername"sv, -errno);
    return {};
}

ErrorOr<void> socketpair(int domain, int type, int protocol, int sv[2])
{
    if (::socketpair(domain, type, protocol, sv) < 0)
        return Error::from_syscall("socketpair"sv, -errno);
    return {};
}

ErrorOr<Array<int, 2>> pipe2([[maybe_unused]] int flags)
{
    Array<int, 2> fds;
#if defined(__unix__)
    if (::pipe2(fds.data(), flags) < 0)
        return Error::from_syscall("pipe2"sv, -errno);
#else
    if (::pipe(fds.data()) < 0)
        return Error::from_syscall("pipe2"sv, -errno);
#endif
    return fds;
}

ErrorOr<Vector<gid_t>> getgroups()
{
    int count = ::getgroups(0, nullptr);
    if (count < 0)
        return Error::from_syscall("getgroups"sv, -errno);
    if (count == 0)
        return Vector<gid_t> {};
    Vector<gid_t> groups;
    TRY(groups.try_resize(count));
    if (::getgroups(count, groups.data()) < 0)
        return Error::from_syscall("getgroups"sv, -errno);
    return groups;
}

ErrorOr<void> mknod(StringView pathname, mode_t mode, dev_t dev)
{
    if (pathname.is_null())
        return Error::from_syscall("mknod"sv, -EFAULT);

#ifdef __serenity__
    Syscall::SC_mknod_params params { { pathname.characters_without_null_termination(), pathname.length() }, mode, dev };
    int rc = syscall(SC_mknod, &params);
    HANDLE_SYSCALL_RETURN_VALUE("mknod"sv, rc, {});
#else
    String path_string = pathname;
    if (::mknod(path_string.characters(), mode, dev) < 0)
        return Error::from_syscall("mknod"sv, -errno);
    return {};
#endif
}

ErrorOr<void> mkfifo(StringView pathname, mode_t mode)
{
    return mknod(pathname, mode | S_IFIFO, 0);
}

}
