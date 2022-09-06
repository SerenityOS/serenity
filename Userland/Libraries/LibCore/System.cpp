/*
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Kenneth Myhra <kennethmyhra@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Matthias Zimmerman <matthias291999@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FixedArray.h>
#include <AK/ScopedValueRollback.h>
#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/File.h>
#include <LibCore/SessionManagement.h>
#include <LibCore/System.h>
#include <limits.h>
#include <stdarg.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/ptrace.h>
#include <sys/time.h>
#include <termios.h>
#include <unistd.h>

#ifdef __serenity__
#    include <LibCore/Account.h>
#    include <LibSystem/syscall.h>
#    include <serenity.h>
#endif

#if defined(__linux__) && !defined(MFD_CLOEXEC)
#    include <linux/memfd.h>
#    include <sys/syscall.h>

static int memfd_create(char const* name, unsigned int flags)
{
    return syscall(SYS_memfd_create, name, flags);
}
#endif

#if defined(__APPLE__)
#    include <sys/mman.h>
#endif

#define HANDLE_SYSCALL_RETURN_VALUE(syscall_name, rc, success_value) \
    if ((rc) < 0) {                                                  \
        return Error::from_syscall(syscall_name##sv, rc);            \
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
    HANDLE_SYSCALL_RETURN_VALUE("pledge", rc, {});
}

ErrorOr<void> unveil(StringView path, StringView permissions)
{
    auto const parsed_path = TRY(Core::SessionManagement::parse_path_with_sid(path));

    Syscall::SC_unveil_params params {
        { parsed_path.characters(), parsed_path.length() },
        { permissions.characters_without_null_termination(), permissions.length() },
    };
    int rc = syscall(SC_unveil, &params);
    HANDLE_SYSCALL_RETURN_VALUE("unveil", rc, {});
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

ErrorOr<void> profiling_enable(pid_t pid, u64 event_mask)
{
    int rc = ::profiling_enable(pid, event_mask);
    HANDLE_SYSCALL_RETURN_VALUE("profiling_enable", rc, {});
}

ErrorOr<void> profiling_disable(pid_t pid)
{
    int rc = ::profiling_disable(pid);
    HANDLE_SYSCALL_RETURN_VALUE("profiling_disable", rc, {});
}

ErrorOr<void> profiling_free_buffer(pid_t pid)
{
    int rc = ::profiling_free_buffer(pid);
    HANDLE_SYSCALL_RETURN_VALUE("profiling_free_buffer", rc, {});
}
#endif

#if !defined(AK_OS_BSD_GENERIC) && !defined(AK_OS_ANDROID)
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

#if defined(__APPLE__) || defined(__OpenBSD__) || defined(__FreeBSD__)
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
    uintptr_t extra_arg = va_arg(ap, uintptr_t);
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

ErrorOr<int> anon_create([[maybe_unused]] size_t size, [[maybe_unused]] int options)
{
    int fd = -1;
#if defined(__serenity__)
    fd = ::anon_create(round_up_to_power_of_two(size, PAGE_SIZE), options);
#elif defined(__linux__) || defined(__FreeBSD__)
    // FIXME: Support more options on Linux.
    auto linux_options = ((options & O_CLOEXEC) > 0) ? MFD_CLOEXEC : 0;
    fd = memfd_create("", linux_options);
    if (fd < 0)
        return Error::from_errno(errno);
    if (::ftruncate(fd, size) < 0) {
        auto saved_errno = errno;
        TRY(close(fd));
        return Error::from_errno(saved_errno);
    }
#elif defined(__APPLE__)
    struct timespec time;
    clock_gettime(CLOCK_REALTIME, &time);
    auto name = String::formatted("/shm-{}{}", (unsigned long)time.tv_sec, (unsigned long)time.tv_nsec);
    fd = shm_open(name.characters(), O_RDWR | O_CREAT | options, 0600);

    if (shm_unlink(name.characters()) == -1) {
        auto saved_errno = errno;
        TRY(close(fd));
        return Error::from_errno(saved_errno);
    }

    if (fd < 0)
        return Error::from_errno(errno);

    if (::ftruncate(fd, size) < 0) {
        auto saved_errno = errno;
        TRY(close(fd));
        return Error::from_errno(saved_errno);
    }

    void* addr = ::mmap(NULL, size, PROT_WRITE, MAP_SHARED, fd, 0);
    if (addr == MAP_FAILED) {
        auto saved_errno = errno;
        TRY(close(fd));
        return Error::from_errno(saved_errno);
    }
#endif
    if (fd < 0)
        return Error::from_errno(errno);
    return fd;
}

ErrorOr<int> open(StringView path, int options, mode_t mode)
{
    return openat(AT_FDCWD, path, options, mode);
}

ErrorOr<int> openat(int fd, StringView path, int options, mode_t mode)
{
    if (!path.characters_without_null_termination())
        return Error::from_syscall("open"sv, -EFAULT);
#ifdef __serenity__
    Syscall::SC_open_params params { fd, { path.characters_without_null_termination(), path.length() }, options, mode };
    int rc = syscall(SC_open, &params);
    HANDLE_SYSCALL_RETURN_VALUE("open", rc, rc);
#else
    // NOTE: We have to ensure that the path is null-terminated.
    String path_string = path;
    int rc = ::openat(fd, path_string.characters(), options, mode);
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
    HANDLE_SYSCALL_RETURN_VALUE("stat", rc, st);
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
    HANDLE_SYSCALL_RETURN_VALUE("lstat", rc, st);
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

ErrorOr<void> killpg(int pgrp, int signal)
{
    if (::killpg(pgrp, signal) < 0)
        return Error::from_syscall("killpg"sv, -errno);
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

ErrorOr<int> tcsetpgrp(int fd, pid_t pgrp)
{
    int rc = ::tcsetpgrp(fd, pgrp);
    if (rc < 0)
        return Error::from_syscall("tcsetpgrp"sv, -errno);
    return { rc };
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
    HANDLE_SYSCALL_RETURN_VALUE("chmod", rc, {});
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

ErrorOr<void> fchown(int fd, uid_t uid, gid_t gid)
{
    if (::fchown(fd, uid, gid) < 0)
        return Error::from_syscall("fchown"sv, -errno);
    return {};
}

ErrorOr<void> lchown(StringView pathname, uid_t uid, gid_t gid)
{
    if (!pathname.characters_without_null_termination())
        return Error::from_syscall("chown"sv, -EFAULT);

#ifdef __serenity__
    Syscall::SC_chown_params params = { { pathname.characters_without_null_termination(), pathname.length() }, uid, gid, AT_FDCWD, false };
    int rc = syscall(SC_chown, &params);
    HANDLE_SYSCALL_RETURN_VALUE("chown", rc, {});
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
    HANDLE_SYSCALL_RETURN_VALUE("chown", rc, {});
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
    HANDLE_SYSCALL_RETURN_VALUE("clocksettime", rc, {});
#else
    if (::clock_settime(clock_id, ts) < 0)
        return Error::from_syscall("clocksettime"sv, -errno);
    return {};
#endif
}

static ALWAYS_INLINE ErrorOr<pid_t> posix_spawn_wrapper(StringView path, posix_spawn_file_actions_t const* file_actions, posix_spawnattr_t const* attr, char* const arguments[], char* const envp[], StringView function_name, decltype(::posix_spawn) spawn_function)
{
    pid_t child_pid;
    if ((errno = spawn_function(&child_pid, path.to_string().characters(), file_actions, attr, arguments, envp)))
        return Error::from_syscall(function_name, -errno);
    return child_pid;
}

ErrorOr<pid_t> posix_spawn(StringView path, posix_spawn_file_actions_t const* file_actions, posix_spawnattr_t const* attr, char* const arguments[], char* const envp[])
{
    return posix_spawn_wrapper(path, file_actions, attr, arguments, envp, "posix_spawn"sv, ::posix_spawn);
}

ErrorOr<pid_t> posix_spawnp(StringView path, posix_spawn_file_actions_t* const file_actions, posix_spawnattr_t* const attr, char* const arguments[], char* const envp[])
{
    return posix_spawn_wrapper(path, file_actions, attr, arguments, envp, "posix_spawnp"sv, ::posix_spawnp);
}

ErrorOr<off_t> lseek(int fd, off_t offset, int whence)
{
    off_t rc = ::lseek(fd, offset, whence);
    if (rc < 0)
        return Error::from_syscall("lseek"sv, -errno);
    return rc;
}

ErrorOr<void> endgrent()
{
    int old_errno = 0;
    swap(old_errno, errno);
    ::endgrent();
    if (errno != 0)
        return Error::from_syscall("endgrent"sv, -errno);
    errno = old_errno;
    return {};
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

ErrorOr<pid_t> getsid(pid_t pid)
{
    int rc = ::getsid(pid);
    if (rc < 0)
        return Error::from_syscall("getsid"sv, -errno);
    return rc;
}

ErrorOr<void> drop_privileges()
{
    auto gid_result = setgid(getgid());
    auto uid_result = setuid(getuid());

    if (gid_result.is_error() || uid_result.is_error())
        return Error::from_string_literal("Failed to drop privileges");

    return {};
}

ErrorOr<bool> isatty(int fd)
{
    int rc = ::isatty(fd);
    if (rc < 0)
        return Error::from_syscall("isatty"sv, -errno);
    return rc == 1;
}

ErrorOr<void> link(StringView old_path, StringView new_path)
{
#ifdef __serenity__
    Syscall::SC_link_params params {
        .old_path = { old_path.characters_without_null_termination(), old_path.length() },
        .new_path = { new_path.characters_without_null_termination(), new_path.length() },
    };
    int rc = syscall(SC_link, &params);
    HANDLE_SYSCALL_RETURN_VALUE("link", rc, {});
#else
    String old_path_string = old_path;
    String new_path_string = new_path;
    if (::link(old_path_string.characters(), new_path_string.characters()) < 0)
        return Error::from_syscall("link"sv, -errno);
    return {};
#endif
}

ErrorOr<void> symlink(StringView target, StringView link_path)
{
#ifdef __serenity__
    Syscall::SC_symlink_params params {
        .target = { target.characters_without_null_termination(), target.length() },
        .linkpath = { link_path.characters_without_null_termination(), link_path.length() },
    };
    int rc = syscall(SC_symlink, &params);
    HANDLE_SYSCALL_RETURN_VALUE("symlink", rc, {});
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
    HANDLE_SYSCALL_RETURN_VALUE("mkdir", rc, {});
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
    HANDLE_SYSCALL_RETURN_VALUE("chdir", rc, {});
#else
    String path_string = path;
    if (::chdir(path_string.characters()) < 0)
        return Error::from_syscall("chdir"sv, -errno);
    return {};
#endif
}

ErrorOr<void> rmdir(StringView path)
{
    if (path.is_null())
        return Error::from_errno(EFAULT);
#ifdef __serenity__
    int rc = syscall(SC_rmdir, path.characters_without_null_termination(), path.length());
    HANDLE_SYSCALL_RETURN_VALUE("rmdir", rc, {});
#else
    String path_string = path;
    if (::rmdir(path_string.characters()) < 0)
        return Error::from_syscall("rmdir"sv, -errno);
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
    HANDLE_SYSCALL_RETURN_VALUE("rename", rc, {});
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
    int rc = syscall(SC_unlink, AT_FDCWD, path.characters_without_null_termination(), path.length(), 0);
    HANDLE_SYSCALL_RETURN_VALUE("unlink", rc, {});
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
    HANDLE_SYSCALL_RETURN_VALUE("utime", rc, {});
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
    HANDLE_SYSCALL_RETURN_VALUE("uname", rc, uts);
#else
    if (::uname(&uts) < 0)
        return Error::from_syscall("uname"sv, -errno);
#endif
    return uts;
}

#ifndef AK_OS_ANDROID
ErrorOr<void> adjtime(const struct timeval* delta, struct timeval* old_delta)
{
#    ifdef __serenity__
    int rc = syscall(SC_adjtime, delta, old_delta);
    HANDLE_SYSCALL_RETURN_VALUE("adjtime", rc, {});
#    else
    if (::adjtime(delta, old_delta) < 0)
        return Error::from_syscall("adjtime"sv, -errno);
    return {};
#    endif
}
#endif

ErrorOr<void> exec(StringView filename, Span<StringView> arguments, SearchInPath search_in_path, Optional<Span<StringView>> environment)
{
#ifdef __serenity__
    Syscall::SC_execve_params params;

    auto argument_strings = TRY(FixedArray<Syscall::StringArgument>::try_create(arguments.size()));
    for (size_t i = 0; i < arguments.size(); ++i) {
        argument_strings[i] = { arguments[i].characters_without_null_termination(), arguments[i].length() };
    }
    params.arguments.strings = argument_strings.data();
    params.arguments.length = argument_strings.size();

    size_t env_count = 0;
    if (environment.has_value()) {
        env_count = environment->size();
    } else {
        for (size_t i = 0; environ[i]; ++i)
            ++env_count;
    }

    auto environment_strings = TRY(FixedArray<Syscall::StringArgument>::try_create(env_count));
    if (environment.has_value()) {
        for (size_t i = 0; i < env_count; ++i) {
            environment_strings[i] = { environment->at(i).characters_without_null_termination(), environment->at(i).length() };
        }
    } else {
        for (size_t i = 0; i < env_count; ++i) {
            environment_strings[i] = { environ[i], strlen(environ[i]) };
        }
    }
    params.environment.strings = environment_strings.data();
    params.environment.length = environment_strings.size();

    auto run_exec = [](Syscall::SC_execve_params& params) -> ErrorOr<void> {
        int rc = syscall(Syscall::SC_execve, &params);
        if (rc < 0)
            return Error::from_syscall("exec"sv, rc);
        return {};
    };

    String exec_filename;

    if (search_in_path == SearchInPath::Yes) {
        auto maybe_executable = Core::File::resolve_executable_from_environment(filename);

        if (!maybe_executable.has_value())
            return ENOENT;

        exec_filename = maybe_executable.release_value();
    } else {
        exec_filename = filename.to_string();
    }

    params.path = { exec_filename.characters(), exec_filename.length() };
    TRY(run_exec(params));
    VERIFY_NOT_REACHED();
#else
    String filename_string { filename };

    auto argument_strings = TRY(FixedArray<String>::try_create(arguments.size()));
    auto argv = TRY(FixedArray<char*>::try_create(arguments.size() + 1));
    for (size_t i = 0; i < arguments.size(); ++i) {
        argument_strings[i] = arguments[i].to_string();
        argv[i] = const_cast<char*>(argument_strings[i].characters());
    }
    argv[arguments.size()] = nullptr;

    int rc = 0;
    if (environment.has_value()) {
        auto environment_strings = TRY(FixedArray<String>::try_create(environment->size()));
        auto envp = TRY(FixedArray<char*>::try_create(environment->size() + 1));
        for (size_t i = 0; i < environment->size(); ++i) {
            environment_strings[i] = environment->at(i).to_string();
            envp[i] = const_cast<char*>(environment_strings[i].characters());
        }
        envp[environment->size()] = nullptr;

        if (search_in_path == SearchInPath::Yes && !filename.contains('/')) {
#    if defined(__APPLE__) || defined(__FreeBSD__)
            // These BSDs don't support execvpe(), so we'll have to manually search the PATH.
            ScopedValueRollback errno_rollback(errno);

            auto maybe_executable = Core::File::resolve_executable_from_environment(filename_string);

            if (!maybe_executable.has_value()) {
                errno_rollback.set_override_rollback_value(ENOENT);
                return Error::from_errno(ENOENT);
            }

            rc = ::execve(maybe_executable.release_value().characters(), argv.data(), envp.data());
#    else
            rc = ::execvpe(filename_string.characters(), argv.data(), envp.data());
#    endif
        } else {
            rc = ::execve(filename_string.characters(), argv.data(), envp.data());
        }

    } else {
        if (search_in_path == SearchInPath::Yes)
            rc = ::execvp(filename_string.characters(), argv.data());
        else
            rc = ::execv(filename_string.characters(), argv.data());
    }

    if (rc < 0)
        return Error::from_syscall("exec"sv, rc);
    VERIFY_NOT_REACHED();
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

ErrorOr<void> setgroups(Span<gid_t const> gids)
{
    if (::setgroups(gids.size(), gids.data()) < 0)
        return Error::from_syscall("setgroups"sv, -errno);
    return {};
}

ErrorOr<void> mknod(StringView pathname, mode_t mode, dev_t dev)
{
    if (pathname.is_null())
        return Error::from_syscall("mknod"sv, -EFAULT);

#ifdef __serenity__
    Syscall::SC_mknod_params params { { pathname.characters_without_null_termination(), pathname.length() }, mode, dev };
    int rc = syscall(SC_mknod, &params);
    HANDLE_SYSCALL_RETURN_VALUE("mknod", rc, {});
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

ErrorOr<void> setenv(StringView name, StringView value, bool overwrite)
{
#ifdef __serenity__
    auto const rc = ::serenity_setenv(name.characters_without_null_termination(), name.length(), value.characters_without_null_termination(), value.length(), overwrite);
#else
    String name_string = name;
    String value_string = value;
    auto const rc = ::setenv(name_string.characters(), value_string.characters(), overwrite);
#endif
    if (rc < 0)
        return Error::from_syscall("setenv"sv, -errno);
    return {};
}

ErrorOr<int> posix_openpt(int flags)
{
    int const rc = ::posix_openpt(flags);
    if (rc < 0)
        return Error::from_syscall("posix_openpt"sv, -errno);
    return rc;
}

ErrorOr<void> grantpt(int fildes)
{
    auto const rc = ::grantpt(fildes);
    if (rc < 0)
        return Error::from_syscall("grantpt"sv, -errno);
    return {};
}

ErrorOr<void> unlockpt(int fildes)
{
    auto const rc = ::unlockpt(fildes);
    if (rc < 0)
        return Error::from_syscall("unlockpt"sv, -errno);
    return {};
}

ErrorOr<void> access(StringView pathname, int mode)
{
    if (pathname.is_null())
        return Error::from_syscall("access"sv, -EFAULT);

#ifdef __serenity__
    int rc = ::syscall(Syscall::SC_access, pathname.characters_without_null_termination(), pathname.length(), mode);
    HANDLE_SYSCALL_RETURN_VALUE("access", rc, {});
#else
    String path_string = pathname;
    if (::access(path_string.characters(), mode) < 0)
        return Error::from_syscall("access"sv, -errno);
    return {};
#endif
}

}
