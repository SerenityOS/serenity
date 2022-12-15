/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/MountFile.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$fsopen(Userspace<Syscall::SC_fsopen_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_no_promises());
    auto credentials = this->credentials();
    if (!credentials->is_superuser())
        return Error::from_errno(EPERM);
    auto params = TRY(copy_typed_from_user(user_params));
    auto fs_type_string = TRY(try_copy_kstring_from_user(params.fs_type));

    // NOTE: If some userspace program uses MS_REMOUNT, return EINVAL to indicate that we never want this
    // flag to appear in the mount table...
    if (params.flags & MS_REMOUNT || params.flags & MS_BIND)
        return Error::from_errno(EINVAL);

    auto mount_file = TRY(MountFile::create(move(fs_type_string), params.flags));
    auto description = TRY(OpenFileDescription::try_create(move(mount_file)));
    return m_fds.with_exclusive([&](auto& fds) -> ErrorOr<FlatPtr> {
        auto new_fd = TRY(fds.allocate());
        fds[new_fd.fd].set(move(description), FD_CLOEXEC);
        return new_fd.fd;
    });
}

ErrorOr<FlatPtr> Process::sys$bindmount(Userspace<Syscall::SC_bindmount_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_no_promises());
    auto credentials = this->credentials();
    if (!credentials->is_superuser())
        return Error::from_errno(EPERM);
    auto params = TRY(copy_typed_from_user(user_params));
    auto source_fd = params.source_fd;
    auto description_or_error = open_file_description(source_fd);
    if (description_or_error.is_error())
        return description_or_error.release_error();
    auto description = description_or_error.release_value();
    if (!description->custody()) {
        // We only support bind-mounting inodes, not arbitrary files.
        return ENODEV;
    }
    auto target = TRY(try_copy_kstring_from_user(params.target));
    auto target_custody = TRY(VirtualFileSystem::the().resolve_path(credentials, target->view(), current_directory()));
    TRY(VirtualFileSystem::the().bind_mount(*description->custody(), target_custody, params.flags));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$remount(Userspace<Syscall::SC_remount_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_no_promises());
    auto credentials = this->credentials();
    if (!credentials->is_superuser())
        return Error::from_errno(EPERM);
    auto params = TRY(copy_typed_from_user(user_params));
    auto target = TRY(try_copy_kstring_from_user(params.target));
    auto target_custody = TRY(VirtualFileSystem::the().resolve_path(credentials, target->view(), current_directory()));
    // We're not creating a new mount, we're updating an existing one!
    TRY(VirtualFileSystem::the().remount(target_custody, params.flags));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$fsmount(Userspace<Syscall::SC_fsmount_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_no_promises());
    auto credentials = this->credentials();
    if (!credentials->is_superuser())
        return Error::from_errno(EPERM);

    auto params = TRY(copy_typed_from_user(user_params));
    auto mount_description = TRY(open_file_description(params.mount_fd));
    if (!mount_description->is_mount_file())
        return Error::from_errno(EINVAL);
    VERIFY(mount_description->mount_file());
    TRY(mount_description->mount_file()->prepared_filesystem().with_exclusive([&](auto& filesystem) -> ErrorOr<void> {
        LockRefPtr<FileSystem> fs;
        auto prepared_filesystem = filesystem;
        if (!prepared_filesystem)
            return Error::from_errno(ENODEV);
        fs = *filesystem;
        auto target = TRY(try_copy_kstring_from_user(params.target));
        auto target_custody = TRY(VirtualFileSystem::the().resolve_path(credentials, target->view(), current_directory()));
        auto flags = mount_description->mount_file()->mount_flags();
        TRY(VirtualFileSystem::the().mount(*fs, target_custody, flags));
        filesystem.clear();
        return {};
    }));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$umount(Userspace<char const*> user_mountpoint, size_t mountpoint_length)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    auto credentials = this->credentials();
    if (!credentials->is_superuser())
        return EPERM;

    TRY(require_no_promises());

    auto mountpoint = TRY(get_syscall_path_argument(user_mountpoint, mountpoint_length));
    auto custody = TRY(VirtualFileSystem::the().resolve_path(credentials, mountpoint->view(), current_directory()));
    TRY(VirtualFileSystem::the().unmount(*custody));
    return 0;
}

}
