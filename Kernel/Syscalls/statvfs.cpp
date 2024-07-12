/*
 * Copyright (c) 2021, Justin Mietzner <sw1tchbl4d3@sw1tchbl4d3.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::do_statvfs(FileSystem const& fs, Custody const* custody, statvfs* buf)
{
    statvfs kernelbuf = {};

    kernelbuf.f_bsize = static_cast<u64>(fs.logical_block_size());
    kernelbuf.f_frsize = fs.fragment_size();
    kernelbuf.f_blocks = fs.total_block_count();
    kernelbuf.f_bfree = fs.free_block_count();

    // FIXME: Implement "available blocks" into Filesystem
    kernelbuf.f_bavail = fs.free_block_count();

    kernelbuf.f_files = fs.total_inode_count();
    kernelbuf.f_ffree = fs.free_inode_count();
    kernelbuf.f_favail = fs.free_inode_count(); // FIXME: same as f_bavail

    kernelbuf.f_fsid = fs.fsid().value();

    kernelbuf.f_namemax = 255;

    (void)fs.class_name().copy_characters_to_buffer(kernelbuf.f_basetype, FSTYPSZ);

    if (custody)
        kernelbuf.f_flag = custody->mount_flags();

    TRY(copy_to_user(buf, &kernelbuf));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$statvfs(Userspace<Syscall::SC_statvfs_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::rpath));
    auto params = TRY(copy_typed_from_user(user_params));

    auto path = TRY(get_syscall_path_argument(params.path));

    auto custody = TRY(VirtualFileSystem::resolve_path(vfs_root_context(), credentials(), path->view(), current_directory(), nullptr, 0));
    auto& inode = custody->inode();
    auto const& fs = inode.fs();

    return do_statvfs(fs, custody, params.buf);
}

ErrorOr<FlatPtr> Process::sys$fstatvfs(int fd, statvfs* buf)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));

    auto description = TRY(open_file_description(fd));
    auto const* inode = description->inode();
    if (inode == nullptr)
        return ENOENT;

    // FIXME: The custody that we pass in might be outdated. However, this only affects the mount flags.
    return do_statvfs(inode->fs(), description->custody(), buf);
}

}
