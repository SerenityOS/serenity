/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/Unshare.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/UnsharedResourceFile.h>
#include <Kernel/FileSystem/VFSRootContext.h>
#include <Kernel/Tasks/HostnameContext.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/ScopedProcessList.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$unshare_open(Userspace<Syscall::SC_unshare_open_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::unshare));

    if (is_jailed())
        return EPERM;

    auto credentials = this->credentials();
    if (!credentials->is_superuser())
        return EPERM;

    auto params = TRY(copy_typed_from_user(user_params));
    if (params.type < 0)
        return EINVAL;

    switch (static_cast<UnshareType>(params.type)) {
    case UnshareType::ScopedProcessList:
    case UnshareType::VFSRootContext:
    case UnshareType::HostnameContext:
        auto new_unshared_resource_file = TRY(UnsharedResourceFile::create(static_cast<UnshareType>(params.type)));
        auto description = TRY(OpenFileDescription::try_create(move(new_unshared_resource_file)));
        return m_fds.with_exclusive([&](auto& fds) -> ErrorOr<FlatPtr> {
            auto new_fd = TRY(fds.allocate());
            fds[new_fd.fd].set(move(description), FD_CLOEXEC);
            return new_fd.fd;
        });
    }
    return ENOTSUP;
}

ErrorOr<FlatPtr> Process::sys$unshare_create(Userspace<Syscall::SC_unshare_create_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::unshare));

    if (is_jailed())
        return EPERM;

    auto credentials = this->credentials();
    if (!credentials->is_superuser())
        return EPERM;

    auto params = TRY(copy_typed_from_user(user_params));
    auto unshared_resource_description = TRY(open_file_description(params.unshared_resource_fd));
    if (!unshared_resource_description->is_unshared_resource_file())
        return EINVAL;

    return TRY(unshared_resource_description->unshared_resource_file()->initialize_resource());
}

ErrorOr<FlatPtr> Process::sys$unshare_enter(Userspace<Syscall::SC_unshare_enter_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::unshare));

    if (is_jailed())
        return EPERM;

    auto credentials = this->credentials();
    if (!credentials->is_superuser())
        return EPERM;

    auto params = TRY(copy_typed_from_user(user_params));
    if (params.type < 0)
        return EINVAL;

    if (params.flags == to_underlying(UnshareEnterFlags::None))
        return EINVAL;

    bool current_program = params.flags & to_underlying(UnshareEnterFlags::CurrentProgram);
    bool after_exec = params.flags & to_underlying(UnshareEnterFlags::AfterExec);

    switch (static_cast<UnshareType>(params.type)) {
    case UnshareType::ScopedProcessList: {
        auto scoped_process_list = TRY(ScopedProcessList::scoped_process_list_for_id(params.id));
        if (current_program)
            replace_scoped_process_list(scoped_process_list);
        if (after_exec)
            m_exec_scoped_process_list.set(scoped_process_list);
        return 0;
    }
    case UnshareType::VFSRootContext: {
        auto vfs_root_context = TRY(vfs_root_context_for_id(params.id));
        if (current_program)
            replace_vfs_root_context(vfs_root_context);
        if (after_exec)
            m_exec_vfs_root_context.set(vfs_root_context);
        return 0;
    }
    case UnshareType::HostnameContext: {
        auto hostname_context = TRY(HostnameContext::hostname_context_for_id(params.id));
        if (current_program)
            replace_hostname_context(hostname_context);
        if (after_exec)
            m_exec_hostname_context.set(hostname_context);
        return 0;
    }
    }
    return ENOTSUP;
}

}
