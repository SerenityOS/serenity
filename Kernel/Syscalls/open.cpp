/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$open(Userspace<const Syscall::SC_open_params*> user_params)
{
    Syscall::SC_open_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;

    int dirfd = params.dirfd;
    int options = params.options;
    u16 mode = params.mode;

    if (options & O_NOFOLLOW_NOERROR)
        return EINVAL;

    if (options & O_UNLINK_INTERNAL)
        return EINVAL;

    if (options & O_WRONLY)
        REQUIRE_PROMISE(wpath);
    else if (options & O_RDONLY)
        REQUIRE_PROMISE(rpath);

    if (options & O_CREAT)
        REQUIRE_PROMISE(cpath);

    // Ignore everything except permission bits.
    mode &= 0777;

    auto path = get_syscall_path_argument(params.path);
    if (path.is_error())
        return path.error();

    dbgln_if(IO_DEBUG, "sys$open(dirfd={}, path='{}', options={}, mode={})", dirfd, path.value()->view(), options, mode);
    int fd = alloc_fd();
    if (fd < 0)
        return fd;

    RefPtr<Custody> base;
    if (dirfd == AT_FDCWD) {
        base = current_directory();
    } else {
        auto base_description = file_description(dirfd);
        if (!base_description)
            return EBADF;
        if (!base_description->is_directory())
            return ENOTDIR;
        if (!base_description->custody())
            return EINVAL;
        base = base_description->custody();
    }

    auto result = VFS::the().open(path.value()->view(), options, mode & ~umask(), *base);
    if (result.is_error())
        return result.error();
    auto description = result.value();

    if (description->inode() && description->inode()->socket())
        return ENXIO;

    u32 fd_flags = (options & O_CLOEXEC) ? FD_CLOEXEC : 0;
    m_fds[fd].set(move(description), fd_flags);
    return fd;
}

KResultOr<int> Process::sys$close(int fd)
{
    REQUIRE_PROMISE(stdio);
    auto description = file_description(fd);
    dbgln_if(IO_DEBUG, "sys$close({}) {}", fd, description.ptr());
    if (!description)
        return EBADF;
    int rc = description->close();
    m_fds[fd] = {};
    return rc;
}

}
