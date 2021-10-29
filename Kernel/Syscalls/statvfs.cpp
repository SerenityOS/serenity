/*
 * Copyright (c) 2021, Justin Mietzner <sw1tchbl4d3@sw1tchbl4d3.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::do_statvfs(StringView path, statvfs* buf)
{
    auto custody = TRY(VirtualFileSystem::the().resolve_path(path, current_directory(), nullptr, 0));
    auto& inode = custody->inode();
    auto& fs = inode.fs();

    statvfs kernelbuf = {};

    kernelbuf.f_bsize = static_cast<u64>(fs.block_size());
    kernelbuf.f_frsize = fs.fragment_size();
    kernelbuf.f_blocks = fs.total_block_count();
    kernelbuf.f_bfree = fs.free_block_count();

    // FIXME: Implement "available blocks" into Filesystem
    kernelbuf.f_bavail = fs.free_block_count();

    kernelbuf.f_files = fs.total_inode_count();
    kernelbuf.f_ffree = fs.free_inode_count();
    kernelbuf.f_favail = fs.free_inode_count(); // FIXME: same as f_bavail

    kernelbuf.f_fsid = 0; // FIXME: Implement "Filesystem ID" into Filesystem

    kernelbuf.f_namemax = 255;

    Custody* current_custody = custody;

    while (current_custody) {
        VirtualFileSystem::the().for_each_mount([&kernelbuf, &current_custody](auto& mount) {
            if (&current_custody->inode() == &mount.guest()) {
                int mountflags = mount.flags();
                int flags = 0;
                if (mountflags & MS_RDONLY)
                    flags = flags | ST_RDONLY;
                if (mountflags & MS_NOSUID)
                    flags = flags | ST_NOSUID;

                kernelbuf.f_flag = flags;
                current_custody = nullptr;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });

        if (current_custody) {
            current_custody = current_custody->parent();
        }
    }

    return copy_to_user(buf, &kernelbuf);
}

KResultOr<FlatPtr> Process::sys$statvfs(Userspace<const Syscall::SC_statvfs_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(rpath);
    auto params = TRY(copy_typed_from_user(user_params));

    auto path = TRY(get_syscall_path_argument(params.path));
    return do_statvfs(path->view(), params.buf);
}

KResultOr<FlatPtr> Process::sys$fstatvfs(int fd, statvfs* buf)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);

    auto description = TRY(fds().open_file_description(fd));
    return do_statvfs(description->absolute_path(), buf);
}

}
