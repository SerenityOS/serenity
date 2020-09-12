/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/DevPtsFS.h>
#include <Kernel/FileSystem/Ext2FileSystem.h>
#include <Kernel/FileSystem/Plan9FileSystem.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/FileSystem/TmpFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

int Process::sys$mount(Userspace<const Syscall::SC_mount_params*> user_params)
{
    if (!is_superuser())
        return -EPERM;

    REQUIRE_NO_PROMISES;

    Syscall::SC_mount_params params;
    if (!copy_from_user(&params, user_params))
        return -EFAULT;

    auto source_fd = params.source_fd;
    auto target = copy_string_from_user(params.target);
    if (target.is_null())
        return -EFAULT;
    auto fs_type = copy_string_from_user(params.fs_type);
    if (fs_type.is_null())
        return -EFAULT;

    auto description = file_description(source_fd);
    if (!description.is_null())
        dbg() << "mount " << fs_type << ": source fd " << source_fd << " @ " << target;
    else
        dbg() << "mount " << fs_type << " @ " << target;

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
            return -EBADF;
        if (!description->custody()) {
            // We only support bind-mounting inodes, not arbitrary files.
            return -ENODEV;
        }
        return VFS::the().bind_mount(*description->custody(), target_custody, params.flags);
    }

    RefPtr<FS> fs;

    if (fs_type == "ext2" || fs_type == "Ext2FS") {
        if (description.is_null())
            return -EBADF;
        if (!description->file().is_seekable()) {
            dbg() << "mount: this is not a seekable file";
            return -ENODEV;
        }

        dbg() << "mount: attempting to mount " << description->absolute_path() << " on " << target;

        fs = Ext2FS::create(*description);
    } else if (fs_type == "9p" || fs_type == "Plan9FS") {
        if (description.is_null())
            return -EBADF;

        fs = Plan9FS::create(*description);
    } else if (fs_type == "proc" || fs_type == "ProcFS") {
        fs = ProcFS::create();
    } else if (fs_type == "devpts" || fs_type == "DevPtsFS") {
        fs = DevPtsFS::create();
    } else if (fs_type == "tmp" || fs_type == "TmpFS") {
        fs = TmpFS::create();
    } else {
        return -ENODEV;
    }

    if (!fs->initialize()) {
        dbg() << "mount: failed to initialize " << fs_type << " filesystem, fd - " << source_fd;
        return -ENODEV;
    }

    auto result = VFS::the().mount(fs.release_nonnull(), target_custody, params.flags);
    if (!description.is_null())
        dbg() << "mount: successfully mounted " << description->absolute_path() << " on " << target;
    else
        dbg() << "mount: successfully mounted " << target;
    return result;
}

int Process::sys$umount(Userspace<const char*> user_mountpoint, size_t mountpoint_length)
{
    if (!is_superuser())
        return -EPERM;

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
