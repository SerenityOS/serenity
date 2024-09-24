/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/InodeWatcherFlags.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/InodeWatcher.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$create_inode_watcher(u32 flags)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::rpath));

    auto watcher = TRY(InodeWatcher::try_create());
    auto description = TRY(OpenFileDescription::try_create(move(watcher)));

    description->set_readable(true);
    if (flags & static_cast<unsigned>(InodeWatcherFlags::Nonblock))
        description->set_blocking(false);

    return m_fds.with_exclusive([&](auto& fds) -> ErrorOr<FlatPtr> {
        auto fd_allocation = TRY(fds.allocate());
        fds[fd_allocation.fd].set(move(description));

        if (flags & static_cast<unsigned>(InodeWatcherFlags::CloseOnExec))
            fds[fd_allocation.fd].set_flags(fds[fd_allocation.fd].flags() | FD_CLOEXEC);

        return fd_allocation.fd;
    });
}

ErrorOr<FlatPtr> Process::sys$inode_watcher_add_watch(Userspace<Syscall::SC_inode_watcher_add_watch_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::rpath));
    auto params = TRY(copy_typed_from_user(user_params));

    auto description = TRY(open_file_description(params.fd));
    if (!description->is_inode_watcher())
        return EBADF;
    auto* inode_watcher = description->inode_watcher();
    auto path = TRY(get_syscall_path_argument(params.user_path));
    auto custody = TRY(VirtualFileSystem::resolve_path(vfs_root_context(), credentials(), path->view(), current_directory()));
    if (!custody->inode().fs().supports_watchers())
        return ENOTSUP;

    return TRY(inode_watcher->register_inode(custody->inode(), params.event_mask));
}

ErrorOr<FlatPtr> Process::sys$inode_watcher_remove_watch(int fd, int wd)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    auto description = TRY(open_file_description(fd));
    if (!description->is_inode_watcher())
        return EBADF;
    TRY(description->inode_watcher()->unregister_by_wd(wd));
    return 0;
}

}
