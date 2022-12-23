/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Userspace.h>
#include <Kernel/Jail.h>
#include <Kernel/JailManagement.h>
#include <Kernel/Process.h>
#include <Kernel/StdLib.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

constexpr size_t jail_name_max_size = 50;

ErrorOr<FlatPtr> Process::sys$jail_create(Userspace<Syscall::SC_jail_create_params*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::jail));

    auto params = TRY(copy_typed_from_user(user_params));
    auto jail_name = TRY(get_syscall_path_argument(params.name));
    if (jail_name->length() > jail_name_max_size)
        return ENAMETOOLONG;

    params.index = TRY(m_attached_jail.with([&](auto& my_jail) -> ErrorOr<u64> {
        // Note: If we are already in a jail, don't let the process to be able to create other jails
        // even if it will not be able to join them later on. The reason for this is to prevent as much as possible
        // any info leak about the "outside world" jail metadata.
        if (my_jail)
            return Error::from_errno(EPERM);
        auto jail = TRY(JailManagement::the().create_jail(move(jail_name)));
        return jail->index().value();
    }));
    // Note: We do the copy_to_user outside of the m_attached_jail Spinlock locked scope because
    // we rely on page faults to work properly.
    TRY(copy_to_user(user_params, &params));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$jail_attach(Userspace<Syscall::SC_jail_attach_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::jail));

    // NOTE: Because the user might run a binary that is using this syscall and
    // that binary was marked as SUID, then the user might be unaware of the
    // fact that while no new setuid binaries might be executed, he is already
    // running within such binary so for the sake of completeness and preventing
    // naive sense of being secure, we should block that.
    TRY(with_protected_data([&](auto& protected_data) -> ErrorOr<void> {
        if (protected_data.executable_is_setid)
            return EPERM;
        return {};
    }));

    auto params = TRY(copy_typed_from_user(user_params));
    return m_attached_jail.with([&](auto& my_jail) -> ErrorOr<FlatPtr> {
        // Note: If we are already in a jail, don't let the process escape it even if
        // it knows there are other jails.
        // Note: To ensure the process doesn't try to maliciously enumerate all jails
        // in the system, just return EPERM before doing anything else.
        if (my_jail)
            return EPERM;
        auto jail = JailManagement::the().find_jail_by_index(static_cast<JailIndex>(params.index));
        if (!jail)
            return EINVAL;
        my_jail = *jail;
        my_jail->attach_count().with([&](auto& attach_count) {
            attach_count++;
        });
        return 0;
    });
}

}
