/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FixedStringBuffer.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/MountFile.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$fsopen(Userspace<Syscall::SC_fsopen_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::mount));
    auto credentials = this->credentials();
    if (!credentials->is_superuser())
        return Error::from_errno(EPERM);
    auto params = TRY(copy_typed_from_user(user_params));
    // NOTE: 16 characters should be enough for any fstype today and in the future.
    auto fs_type_string = TRY(get_syscall_name_string_fixed_buffer<16>(params.fs_type));

    // NOTE: If some userspace program uses MS_REMOUNT, return EINVAL to indicate that we never want this
    // flag to appear in the mount table...
    if (params.flags & MS_REMOUNT || params.flags & MS_BIND)
        return Error::from_errno(EINVAL);

    auto const* fs_type_initializer = TRY(VirtualFileSystem::find_filesystem_type_initializer(fs_type_string.representable_view()));
    VERIFY(fs_type_initializer);
    auto mount_file = TRY(MountFile::create(*fs_type_initializer, params.flags));
    auto description = TRY(OpenFileDescription::try_create(move(mount_file)));
    return m_fds.with_exclusive([&](auto& fds) -> ErrorOr<FlatPtr> {
        auto new_fd = TRY(fds.allocate());
        fds[new_fd.fd].set(move(description), FD_CLOEXEC);
        return new_fd.fd;
    });
}

ErrorOr<FlatPtr> Process::sys$fsmount(Userspace<Syscall::SC_fsmount_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::mount));
    auto credentials = this->credentials();
    if (!credentials->is_superuser())
        return Error::from_errno(EPERM);

    auto params = TRY(copy_typed_from_user(user_params));
    auto mount_description = TRY(open_file_description(params.mount_fd));
    if (!mount_description->is_mount_file())
        return Error::from_errno(EINVAL);

    RefPtr<OpenFileDescription> source_description = TRY(open_file_description_ignoring_negative(params.source_fd));
    auto target = TRY(try_copy_kstring_from_user(params.target));
    auto target_custody = TRY(VirtualFileSystem::the().resolve_path(credentials, target->view(), current_directory()));
    auto flags = mount_description->mount_file()->mount_flags();
    TRY(VirtualFileSystem::the().mount(*mount_description->mount_file(), source_description.ptr(), target_custody, flags));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$remount(Userspace<Syscall::SC_remount_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::mount));
    auto credentials = this->credentials();
    if (!credentials->is_superuser())
        return EPERM;

    auto params = TRY(copy_typed_from_user(user_params));
    if (params.flags & MS_REMOUNT)
        return EINVAL;
    if (params.flags & MS_BIND)
        return EINVAL;

    auto target = TRY(try_copy_kstring_from_user(params.target));
    auto target_custody = TRY(VirtualFileSystem::the().resolve_path(credentials, target->view(), current_directory()));
    TRY(VirtualFileSystem::the().remount(target_custody, params.flags));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$bindmount(Userspace<Syscall::SC_bindmount_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::mount));
    auto credentials = this->credentials();
    if (!credentials->is_superuser())
        return EPERM;

    auto params = TRY(copy_typed_from_user(user_params));
    if (params.flags & MS_REMOUNT)
        return EINVAL;
    if (params.flags & MS_BIND)
        return EINVAL;

    auto source_fd = params.source_fd;
    auto target = TRY(try_copy_kstring_from_user(params.target));
    auto target_custody = TRY(VirtualFileSystem::the().resolve_path(credentials, target->view(), current_directory()));

    auto description = TRY(open_file_description(source_fd));
    if (!description->custody()) {
        // NOTE: We only support bind-mounting inodes, not arbitrary files.
        return ENODEV;
    }

    TRY(VirtualFileSystem::the().bind_mount(*description->custody(), target_custody, params.flags));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$umount(Userspace<char const*> user_mountpoint, size_t mountpoint_length)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    auto credentials = this->credentials();
    if (!credentials->is_superuser())
        return EPERM;

    TRY(require_promise(Pledge::mount));

    auto mountpoint = TRY(get_syscall_path_argument(user_mountpoint, mountpoint_length));
    auto custody = TRY(VirtualFileSystem::the().resolve_path(credentials, mountpoint->view(), current_directory()));
    TRY(VirtualFileSystem::the().unmount(*custody));
    return 0;
}

}
