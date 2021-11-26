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
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$create_inode_watcher(u32 flags)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(rpath);

    auto fd_allocation = TRY(m_fds.allocate());
    auto watcher = TRY(InodeWatcher::try_create());
    auto description = TRY(OpenFileDescription::try_create(move(watcher)));

    description->set_readable(true);
    if (flags & static_cast<unsigned>(InodeWatcherFlags::Nonblock))
        description->set_blocking(false);

    m_fds[fd_allocation.fd].set(move(description));

    if (flags & static_cast<unsigned>(InodeWatcherFlags::CloseOnExec))
        m_fds[fd_allocation.fd].set_flags(m_fds[fd_allocation.fd].flags() | FD_CLOEXEC);

    return fd_allocation.fd;
}

ErrorOr<FlatPtr> Process::sys$inode_watcher_add_watch(Userspace<const Syscall::SC_inode_watcher_add_watch_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(rpath);
    auto params = TRY(copy_typed_from_user(user_params));

    auto description = TRY(fds().open_file_description(params.fd));
    if (!description->is_inode_watcher())
        return EBADF;
    auto inode_watcher = description->inode_watcher();
    auto path = TRY(get_syscall_path_argument(params.user_path));
    auto custody = TRY(VirtualFileSystem::the().resolve_path(path->view(), current_directory()));
    if (!custody->inode().fs().supports_watchers())
        return ENOTSUP;

    return TRY(inode_watcher->register_inode(custody->inode(), params.event_mask));
}

ErrorOr<FlatPtr> Process::sys$inode_watcher_remove_watch(int fd, int wd)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    auto description = TRY(fds().open_file_description(fd));
    if (!description->is_inode_watcher())
        return EBADF;
    TRY(description->inode_watcher()->unregister_by_wd(wd));
    return 0;
}

}
