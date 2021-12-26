/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/DevPtsFS.h>
#include <Kernel/FileSystem/DevTmpFS.h>
#include <Kernel/FileSystem/Ext2FileSystem.h>
#include <Kernel/FileSystem/ISO9660FileSystem.h>
#include <Kernel/FileSystem/Plan9FileSystem.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/FileSystem/SysFS.h>
#include <Kernel/FileSystem/TmpFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$mount(Userspace<const Syscall::SC_mount_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    if (!is_superuser())
        return EPERM;

    REQUIRE_NO_PROMISES;
    auto params = TRY(copy_typed_from_user(user_params));

    auto source_fd = params.source_fd;
    auto target = TRY(try_copy_kstring_from_user(params.target));
    auto fs_type_string = TRY(try_copy_kstring_from_user(params.fs_type));
    auto fs_type = fs_type_string->view();

    auto description_or_error = fds().open_file_description(source_fd);
    if (!description_or_error.is_error())
        dbgln("mount {}: source fd {} @ {}", fs_type, source_fd, target);
    else
        dbgln("mount {} @ {}", fs_type, target);

    auto target_custody = TRY(VirtualFileSystem::the().resolve_path(target->view(), current_directory()));

    if (params.flags & MS_REMOUNT) {
        // We're not creating a new mount, we're updating an existing one!
        return VirtualFileSystem::the().remount(target_custody, params.flags & ~MS_REMOUNT);
    }

    if (params.flags & MS_BIND) {
        // We're doing a bind mount.
        if (description_or_error.is_error())
            return description_or_error.error();
        auto description = description_or_error.release_value();
        if (!description->custody()) {
            // We only support bind-mounting inodes, not arbitrary files.
            return ENODEV;
        }
        return VirtualFileSystem::the().bind_mount(*description->custody(), target_custody, params.flags);
    }

    RefPtr<FileSystem> fs;

    if (fs_type == "ext2"sv || fs_type == "Ext2FS"sv) {
        if (description_or_error.is_error())
            return EBADF;
        auto description = description_or_error.release_value();
        if (!description->file().is_block_device())
            return ENOTBLK;
        if (!description->file().is_seekable()) {
            dbgln("mount: this is not a seekable file");
            return ENODEV;
        }

        dbgln("mount: attempting to mount {} on {}", description->absolute_path(), target);

        fs = TRY(Ext2FS::try_create(*description));
    } else if (fs_type == "9p"sv || fs_type == "Plan9FS"sv) {
        if (description_or_error.is_error())
            return EBADF;
        auto description = description_or_error.release_value();
        fs = TRY(Plan9FS::try_create(*description));
    } else if (fs_type == "proc"sv || fs_type == "ProcFS"sv) {
        fs = TRY(ProcFS::try_create());
    } else if (fs_type == "devpts"sv || fs_type == "DevPtsFS"sv) {
        fs = TRY(DevPtsFS::try_create());
    } else if (fs_type == "dev"sv || fs_type == "DevTmpFS"sv) {
        fs = TRY(DevTmpFS::try_create());
    } else if (fs_type == "sys"sv || fs_type == "SysFS"sv) {
        fs = TRY(SysFS::try_create());
    } else if (fs_type == "tmp"sv || fs_type == "TmpFS"sv) {
        fs = TRY(TmpFS::try_create());
    } else if (fs_type == "iso9660"sv || fs_type == "ISO9660FS"sv) {
        if (description_or_error.is_error())
            return EBADF;
        auto description = description_or_error.release_value();
        if (!description->file().is_seekable()) {
            dbgln("mount: this is not a seekable file");
            return ENODEV;
        }
        dbgln("mount: attempting to mount {} on {}", description->absolute_path(), target);
        fs = TRY(ISO9660FS::try_create(*description));
    } else {
        return ENODEV;
    }

    if (!fs)
        return ENOMEM;

    TRY(fs->initialize());
    return VirtualFileSystem::the().mount(fs.release_nonnull(), target_custody, params.flags);
}

KResultOr<FlatPtr> Process::sys$umount(Userspace<const char*> user_mountpoint, size_t mountpoint_length)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    if (!is_superuser())
        return EPERM;

    REQUIRE_NO_PROMISES;

    auto mountpoint = TRY(get_syscall_path_argument(user_mountpoint, mountpoint_length));
    auto custody = TRY(VirtualFileSystem::the().resolve_path(mountpoint->view(), current_directory()));
    auto& guest_inode = custody->inode();
    return VirtualFileSystem::the().unmount(guest_inode);
}

}
