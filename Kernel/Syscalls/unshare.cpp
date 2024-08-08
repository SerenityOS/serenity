/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/Unshare.h>
#include <Kernel/FileSystem/VFSRootContext.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/ScopedProcessList.h>

namespace Kernel {

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
    if (params.type < 0)
        return EINVAL;

    switch (static_cast<UnshareType>(params.type)) {
    case UnshareType::ScopedProcessList: {
        auto new_process_list = TRY(ScopedProcessList::create());
        return new_process_list->id().value();
    }
    case UnshareType::VFSRootContext: {
        auto new_vfs_root_context = TRY(VFSRootContext::create_with_empty_ramfs());
        return new_vfs_root_context->id().value();
    }
    case UnshareType::HostnameContext: {
        TRY(m_attached_hostname_context.with([](auto& context) -> ErrorOr<void> {
            FixedStringBuffer<UTSNAME_ENTRY_LEN - 1> hostname;
            context->buffer().with([&hostname](auto& buffer) {
                hostname.store_characters(buffer.representable_view());
            });
            // NOTE: Create a new context, based on the contents of previous attached one.
            context = TRY(HostnameContext::create_with_name(hostname.representable_view()));
            return {};
        }));
        return 0;
    }
    }
    return Error::from_errno(ENOTSUP);
}

ErrorOr<FlatPtr> Process::sys$unshare_attach(Userspace<Syscall::SC_unshare_attach_params const*> user_params)
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
    case UnshareType::ScopedProcessList: {
        auto scoped_process_list = TRY(ScopedProcessList::scoped_process_list_for_id(params.id));
        m_scoped_process_list.with([this, scoped_process_list](auto& list_ptr) {
            list_ptr = scoped_process_list;
            list_ptr->attach(*this);
        });
        return 0;
    }
    case UnshareType::VFSRootContext: {
        auto vfs_root_context = TRY(vfs_root_context_for_id(params.id));
        m_attached_vfs_root_context.with([vfs_root_context](auto& context) {
            context = vfs_root_context;
        });
        vfs_root_context->attach({});
        return 0;
    }
    case UnshareType::HostnameContext: {
        auto hostname_context = TRY(HostnameContext::hostname_context_for_id(params.id));
        m_attached_hostname_context.with([hostname_context](auto& context) {
            context = hostname_context;
        });
        hostname_context->set_attached({});
        return 0;
    }
    }
    return Error::from_errno(ENOTSUP);
}

}
