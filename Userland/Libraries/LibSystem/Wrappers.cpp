/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSystem/Wrappers.h>
#include <LibSystem/syscall.h>

#define HANDLE_SYSCALL_RETURN_VALUE(syscall_name, rc, success_value) \
    if ((rc) < 0) {                                                  \
        return Error::from_syscall(syscall_name, rc);                \
    }                                                                \
    return success_value;

namespace System {

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

ErrorOr<void> sigaction(int signal, struct sigaction const* action, struct sigaction* old_action)
{
    int rc = syscall(SC_sigaction, signal, action, old_action);
    HANDLE_SYSCALL_RETURN_VALUE("sigaction"sv, rc, {});
}

ErrorOr<struct stat> fstat(int fd)
{
    struct stat st;
    int rc = syscall(SC_fstat, fd, &st);
    HANDLE_SYSCALL_RETURN_VALUE("fstat"sv, rc, st);
}

}
