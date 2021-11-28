/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Kenneth Myhra <kennethmyhra@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/System.h>
#include <LibSystem/syscall.h>
#include <fcntl.h>
#include <stdarg.h>
#include <sys/mman.h>
#include <sys/socket.h>
#include <unistd.h>

#define HANDLE_SYSCALL_RETURN_VALUE(syscall_name, rc, success_value) \
    if ((rc) < 0) {                                                  \
        return Error::from_syscall(syscall_name, rc);                \
    }                                                                \
    return success_value;

namespace Core::System {

#ifdef __serenity__
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

ErrorOr<Array<int, 2>> pipe2(int flags)
{
    Array<int, 2> fds;
    if (::pipe2(fds.data(), flags) < 0)
        return Error::from_syscall("pipe2"sv, -errno);
    return fds;
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
#endif

ErrorOr<void> sigaction(int signal, struct sigaction const* action, struct sigaction* old_action)
{
    if (::sigaction(signal, action, old_action) < 0)
        return Error::from_syscall("sigaction"sv, -errno);
    return {};
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
    Syscall::SC_mmap_params params { (uintptr_t)address, size, alignment, protection, flags, fd, offset, { name.characters_without_null_termination(), name.length() } };
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
    char hostname[256];
    int rc = ::gethostname(hostname, sizeof(hostname));
    if (rc < 0)
        return Error::from_syscall("gethostname"sv, -errno);
    return String(&hostname[0]);
}

}
