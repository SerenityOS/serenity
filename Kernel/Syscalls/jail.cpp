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

    auto jail = TRY(JailManagement::the().create_jail(move(jail_name)));
    params.index = jail->index().value();

    TRY(copy_to_user(user_params, &params));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$jail_attach(Userspace<Syscall::SC_jail_attach_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::jail));

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
