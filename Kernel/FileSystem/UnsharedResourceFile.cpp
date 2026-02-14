/*
 * Copyright (c) 2026, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StdLibExtras.h>
#include <AK/StringView.h>
#include <Kernel/API/Ioctl.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/API/Syscall.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/UnsharedResourceFile.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Library/StdLib.h>
#include <Kernel/Tasks/Process.h>

#include <Kernel/FileSystem/VFSRootContext.h>
#include <Kernel/Tasks/HostnameContext.h>
#include <Kernel/Tasks/ScopedProcessList.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<UnsharedResourceFile>> UnsharedResourceFile::create(UnshareType type)
{
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) UnsharedResourceFile(type)));
}

UnsharedResourceFile::UnsharedResourceFile(UnshareType type)
    : m_type(type)
{
}

ErrorOr<unsigned> UnsharedResourceFile::initialize_resource()
{
    switch (m_type) {
    case UnshareType::ScopedProcessList: {
        auto scoped_process_list = TRY(ScopedProcessList::create());
        return scoped_process_list->id().value();
    }
    case UnshareType::VFSRootContext: {
        return m_root_filesystem.with_exclusive([&](auto& root_filesystem) -> ErrorOr<unsigned> {
            if (root_filesystem) {
                auto vfs_root_context = TRY(VFSRootContext::create_with_filesystem(VFSRootContext::AddToGlobalContextList::Yes, *root_filesystem));
                return vfs_root_context->id().value();
            }
            auto vfs_root_context = TRY(VFSRootContext::create_with_empty_ramfs(VFSRootContext::AddToGlobalContextList::Yes));
            return vfs_root_context->id().value();
        });
    }
    case UnshareType::HostnameContext: {
        // NOTE: Technically, we create a new context, based on the
        // contents of "previous" attached one.
        // However, the process can change the hostname context during
        // this call as we don't really hold a lock on it, but that's
        // really a non-issue because it's not destructive or really harmful
        // and if a process is so **eager** to blindly change hostname contexts
        // with multiple fds, we should not be the ones to stop it from doing that.
        auto current_context = Process::current().hostname_context();
        FixedStringBuffer<UTSNAME_ENTRY_LEN - 1> hostname;
        current_context->buffer().with([&hostname](auto& buffer) {
            hostname.store_characters(buffer.representable_view());
        });
        auto hostname_context = TRY(HostnameContext::create_with_name(hostname.representable_view()));
        return hostname_context->id().value();
    }
    }
    return ENOTSUP;
}

UnsharedResourceFile::~UnsharedResourceFile() = default;

ErrorOr<void> UnsharedResourceFile::ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg)
{
    switch (request) {
    case UNSHARE_IOCTL_ATTACH_ROOT_FILESYSTEM_AT_FD: {
        auto current_vfs_root_context = Process::current().vfs_root_context();
        auto credentials = Process::current().credentials();
        auto fd = static_cast<int>(arg.ptr());
        auto description = TRY(Process::current().open_file_description_ignoring_negative(fd));
        return m_root_filesystem.with_exclusive([&credentials, &current_vfs_root_context, &description](auto& root_filesystem) -> ErrorOr<void> {
            if (!description) {
                root_filesystem = nullptr;
                return {};
            }

            if (!description->is_directory())
                return ENOTDIR;
            if (!description->metadata().may_execute(credentials))
                return EACCES;
            VERIFY(description->custody());
            root_filesystem = TRY(current_vfs_root_context->mount_point_to_guest_filesystem(*description->custody()));
            return {};
        });
    }

    default:
        return EINVAL;
    }
}

ErrorOr<NonnullOwnPtr<KString>> UnsharedResourceFile::pseudo_path(OpenFileDescription const&) const
{
    return KString::try_create(":unshared-resource-file:"sv);
}

}
