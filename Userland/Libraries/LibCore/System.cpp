/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibSystem/syscall.h>
#include <fcntl.h>
#include <stdarg.h>

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

}
