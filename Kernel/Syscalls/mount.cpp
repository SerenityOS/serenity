/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/DevPtsFS.h>
#include <Kernel/FileSystem/Ext2FileSystem.h>
#include <Kernel/FileSystem/FATFileSystem.h>
#include <Kernel/FileSystem/ISO9660FileSystem.h>
#include <Kernel/FileSystem/Plan9FileSystem.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/FileSystem/SysFS.h>
#include <Kernel/FileSystem/TmpFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

struct FileSystemInitializer {
    StringView short_name;
    StringView name;
    bool requires_open_file_description { false };
    bool requires_block_device { false };
    bool requires_seekable_file { false };
    ErrorOr<NonnullLockRefPtr<FileSystem>> (*create_with_fd)(OpenFileDescription&) = nullptr;
    ErrorOr<NonnullLockRefPtr<FileSystem>> (*create)(void) = nullptr;
};

static constexpr FileSystemInitializer s_initializers[] = {
    { "proc"sv, "ProcFS"sv, false, false, false, {}, ProcFS::try_create },
    { "devpts"sv, "DevPtsFS"sv, false, false, false, {}, DevPtsFS::try_create },
    { "sys"sv, "SysFS"sv, false, false, false, {}, SysFS::try_create },
    { "tmp"sv, "TmpFS"sv, false, false, false, {}, TmpFS::try_create },
    { "ext2"sv, "Ext2FS"sv, true, true, true, Ext2FS::try_create, {} },
    { "9p"sv, "Plan9FS"sv, true, true, true, Plan9FS::try_create, {} },
    { "iso9660"sv, "ISO9660FS"sv, true, true, true, ISO9660FS::try_create, {} },
    { "fat"sv, "FATFS"sv, true, true, true, FATFS::try_create, {} },
};

static ErrorOr<NonnullLockRefPtr<FileSystem>> find_or_create_filesystem_instance(StringView fs_type, OpenFileDescription* possible_description)
{
    for (auto& initializer_entry : s_initializers) {
        if (fs_type != initializer_entry.short_name && fs_type != initializer_entry.name)
            continue;
        if (!initializer_entry.requires_open_file_description) {
            VERIFY(initializer_entry.create);
            NonnullLockRefPtr<FileSystem> fs = TRY(initializer_entry.create());
            return fs;
        }
        // Note: If there's an associated file description with the filesystem, we could
        // try to first find it from the VirtualFileSystem filesystem list and if it was not found,
        // then create it and add it.
        VERIFY(initializer_entry.create_with_fd);
        if (!possible_description)
            return EBADF;
        OpenFileDescription& description = *possible_description;

        if (initializer_entry.requires_block_device && !description.file().is_block_device())
            return ENOTBLK;
        if (initializer_entry.requires_seekable_file && !description.file().is_seekable()) {
            dbgln("mount: this is not a seekable file");
            return ENODEV;
        }
        return TRY(VirtualFileSystem::the().find_already_existing_or_create_file_backed_file_system(description, initializer_entry.create_with_fd));
    }
    return ENODEV;
}

ErrorOr<FlatPtr> Process::sys$mount(Userspace<Syscall::SC_mount_params const*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_no_promises());
    auto credentials = this->credentials();
    if (!credentials->is_superuser())
        return EPERM;

    auto params = TRY(copy_typed_from_user(user_params));

    auto source_fd = params.source_fd;
    auto target = TRY(try_copy_kstring_from_user(params.target));
    auto fs_type_string = TRY(try_copy_kstring_from_user(params.fs_type));
    auto fs_type = fs_type_string->view();

    auto description_or_error = open_file_description(source_fd);
    if (!description_or_error.is_error())
        dbgln("mount {}: source fd {} @ {}", fs_type, source_fd, target);
    else
        dbgln("mount {} @ {}", fs_type, target);

    auto target_custody = TRY(VirtualFileSystem::the().resolve_path(credentials, target->view(), current_directory()));

    if (params.flags & MS_REMOUNT) {
        // We're not creating a new mount, we're updating an existing one!
        TRY(VirtualFileSystem::the().remount(target_custody, params.flags & ~MS_REMOUNT));
        return 0;
    }

    if (params.flags & MS_BIND) {
        // We're doing a bind mount.
        if (description_or_error.is_error())
            return description_or_error.release_error();
        auto description = description_or_error.release_value();
        if (!description->custody()) {
            // We only support bind-mounting inodes, not arbitrary files.
            return ENODEV;
        }
        TRY(VirtualFileSystem::the().bind_mount(*description->custody(), target_custody, params.flags));
        return 0;
    }

    LockRefPtr<FileSystem> fs;

    if (!description_or_error.is_error()) {
        auto description = description_or_error.release_value();
        fs = TRY(find_or_create_filesystem_instance(fs_type, description.ptr()));
        auto source_pseudo_path = TRY(description->pseudo_path());
        dbgln("mount: attempting to mount {} on {}", source_pseudo_path, target);
    } else {
        fs = TRY(find_or_create_filesystem_instance(fs_type, {}));
    }

    TRY(fs->initialize());
    TRY(VirtualFileSystem::the().mount(*fs, target_custody, params.flags));
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
