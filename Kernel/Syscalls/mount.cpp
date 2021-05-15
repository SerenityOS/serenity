/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/DevFS.h>
#include <Kernel/FileSystem/DevPtsFS.h>
#include <Kernel/FileSystem/Ext2FileSystem.h>
#include <Kernel/FileSystem/Plan9FileSystem.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/FileSystem/TmpFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$mount(Userspace<const Syscall::SC_mount_params*> user_params)
{
    if (!is_superuser())
        return EPERM;

    REQUIRE_NO_PROMISES;

    Syscall::SC_mount_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;

    auto source_fd = params.source_fd;
    auto target = copy_string_from_user(params.target);
    if (target.is_null())
        return EFAULT;
    auto fs_type = copy_string_from_user(params.fs_type);
    if (fs_type.is_null())
        return EFAULT;

    auto description = file_description(source_fd);
    if (!description.is_null())
        dbgln("mount {}: source fd {} @ {}", fs_type, source_fd, target);
    else
        dbgln("mount {} @ {}", fs_type, target);

    auto custody_or_error = VFS::the().resolve_path(target, current_directory());
    if (custody_or_error.is_error())
        return custody_or_error.error();

    auto& target_custody = custody_or_error.value();

    if (params.flags & MS_REMOUNT) {
        // We're not creating a new mount, we're updating an existing one!
        return VFS::the().remount(target_custody, params.flags & ~MS_REMOUNT);
    }

    if (params.flags & MS_BIND) {
        // We're doing a bind mount.
        if (description.is_null())
            return EBADF;
        if (!description->custody()) {
            // We only support bind-mounting inodes, not arbitrary files.
            return ENODEV;
        }
        return VFS::the().bind_mount(*description->custody(), target_custody, params.flags);
    }

    RefPtr<FS> fs;

    if (fs_type == "ext2" || fs_type == "Ext2FS") {
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
    } else if (fs_type == "9p" || fs_type == "Plan9FS") {
        if (description.is_null())
            return EBADF;

        fs = Plan9FS::create(*description);
    } else if (fs_type == "proc" || fs_type == "ProcFS") {
        fs = ProcFS::create();
    } else if (fs_type == "devpts" || fs_type == "DevPtsFS") {
        fs = DevPtsFS::create();
    } else if (fs_type == "dev" || fs_type == "DevFS") {
        fs = DevFS::create();
    } else if (fs_type == "tmp" || fs_type == "TmpFS") {
        fs = TmpFS::create();
    } else {
        return ENODEV;
    }

    if (!fs->initialize()) {
        dbgln("mount: failed to initialize {} filesystem, fd={}", fs_type, source_fd);
        return ENODEV;
    }

    auto result = VFS::the().mount(fs.release_nonnull(), target_custody, params.flags);
    if (!description.is_null())
        dbgln("mount: successfully mounted {} on {}", description->absolute_path(), target);
    else
        dbgln("mount: successfully mounted {}", target);
    return result;
}

KResultOr<int> Process::sys$umount(Userspace<const char*> user_mountpoint, size_t mountpoint_length)
{
    if (!is_superuser())
        return EPERM;

    REQUIRE_NO_PROMISES;

    auto mountpoint = get_syscall_path_argument(user_mountpoint, mountpoint_length);
    if (mountpoint.is_error())
        return mountpoint.error();

    auto custody_or_error = VFS::the().resolve_path(mountpoint.value(), current_directory());
    if (custody_or_error.is_error())
        return custody_or_error.error();

    auto& guest_inode = custody_or_error.value()->inode();
    return VFS::the().unmount(guest_inode);
}

}
