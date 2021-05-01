/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/InodeWatcher.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$watch_file(Userspace<const char*> user_path, size_t path_length)
{
    REQUIRE_PROMISE(rpath);
    auto path = get_syscall_path_argument(user_path, path_length);
    if (path.is_error())
        return path.error();

    auto custody_or_error = VFS::the().resolve_path(path.value(), current_directory());
    if (custody_or_error.is_error())
        return custody_or_error.error();

    auto& custody = custody_or_error.value();
    auto& inode = custody->inode();

    if (!inode.fs().supports_watchers())
        return ENOTSUP;

    int fd = alloc_fd();
    if (fd < 0)
        return fd;

    auto description = FileDescription::create(*InodeWatcher::create(inode));
    if (description.is_error())
        return description.error();

    m_fds[fd].set(description.release_value());
    m_fds[fd].description()->set_readable(true);
    return fd;
}

}
