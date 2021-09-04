/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/DevFS.h>
#include <Kernel/FileSystem/DevPtsFS.h>
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

    Syscall::SC_mount_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;

    auto source_fd = params.source_fd;
    auto target_or_error = try_copy_kstring_from_user(params.target);
    if (target_or_error.is_error())
        return target_or_error.error();
    auto fs_type_or_error = try_copy_kstring_from_user(params.fs_type);
    if (fs_type_or_error.is_error())
        return fs_type_or_error.error();

    auto target = target_or_error.value()->view();
    auto fs_type = fs_type_or_error.value()->view();

    auto description = fds().file_description(source_fd);
    if (!description.is_null())
        dbgln("mount {}: source fd {} @ {}", fs_type, source_fd, target);
    else
        dbgln("mount {} @ {}", fs_type, target);

    auto custody_or_error = VirtualFileSystem::the().resolve_path(target, current_directory());
    if (custody_or_error.is_error())
        return custody_or_error.error();

    auto& target_custody = custody_or_error.value();

    if (params.flags & MS_REMOUNT) {
        // We're not creating a new mount, we're updating an existing one!
        return VirtualFileSystem::the().remount(target_custody, params.flags & ~MS_REMOUNT);
    }

    if (params.flags & MS_BIND) {
        // We're doing a bind mount.
        if (description.is_null())
            return EBADF;
        if (!description->custody()) {
            // We only support bind-mounting inodes, not arbitrary files.
            return ENODEV;
        }
        return VirtualFileSystem::the().bind_mount(*description->custody(), target_custody, params.flags);
    }

    RefPtr<FileSystem> fs;

    if (fs_type == "ext2"sv || fs_type == "Ext2FS"sv) {
        if (description.is_null())
            return EBADF;
        if (!description->file().is_block_device())
            return ENOTBLK;
        if (!description->file().is_seekable()) {
            dbgln("mount: this is not a seekable file");
            return ENODEV;
        }

        dbgln("mount: attempting to mount {} on {}", description->absolute_path(), target);

        fs = Ext2FS::create(*description);
    } else if (fs_type == "9p"sv || fs_type == "Plan9FS"sv) {
        if (description.is_null())
            return EBADF;

        fs = Plan9FS::create(*description);
    } else if (fs_type == "proc"sv || fs_type == "ProcFS"sv) {
        auto maybe_fs = ProcFS::try_create();
        if (maybe_fs.is_error())
            return maybe_fs.error();
        fs = maybe_fs.release_value();
    } else if (fs_type == "devpts"sv || fs_type == "DevPtsFS"sv) {
        fs = DevPtsFS::create();
    } else if (fs_type == "dev"sv || fs_type == "DevFS"sv) {
        fs = DevFS::create();
    } else if (fs_type == "sys"sv || fs_type == "SysFS"sv) {
        fs = SysFS::create();
    } else if (fs_type == "tmp"sv || fs_type == "TmpFS"sv) {
        fs = TmpFS::create();
    } else if (fs_type == "iso9660"sv || fs_type == "ISO9660FS"sv) {
        if (description.is_null())
            return EBADF;
        if (!description->file().is_seekable()) {
            dbgln("mount: this is not a seekable file");
            return ENODEV;
        }

        dbgln("mount: attempting to mount {} on {}", description->absolute_path(), target);

        auto maybe_fs = ISO9660FS::try_create(*description);
        if (maybe_fs.is_error()) {
            return maybe_fs.error();
        }
        fs = maybe_fs.release_value();
    } else {
        return ENODEV;
    }

    if (!fs)
        return ENOMEM;

    if (auto result = fs->initialize(); result.is_error()) {
        dbgln("mount: failed to initialize {} filesystem, fd={}", fs_type, source_fd);
        return result;
    }

    auto result = VirtualFileSystem::the().mount(fs.release_nonnull(), target_custody, params.flags);
    if (!description.is_null())
        dbgln("mount: successfully mounted {} on {}", description->absolute_path(), target);
    else
        dbgln("mount: successfully mounted {}", target);
    return result;
}

KResultOr<FlatPtr> Process::sys$umount(Userspace<char const*> user_mountpoint, size_t mountpoint_length)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    if (!is_superuser())
        return EPERM;

    REQUIRE_NO_PROMISES;

    auto mountpoint = get_syscall_path_argument(user_mountpoint, mountpoint_length);
    if (mountpoint.is_error())
        return mountpoint.error();

    auto custody_or_error = VirtualFileSystem::the().resolve_path(mountpoint.value()->view(), current_directory());
    if (custody_or_error.is_error())
        return custody_or_error.error();

    auto& guest_inode = custody_or_error.value()->inode();
    return VirtualFileSystem::the().unmount(guest_inode);
}

}
