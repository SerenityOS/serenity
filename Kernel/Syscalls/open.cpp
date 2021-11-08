/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$open(Userspace<const Syscall::SC_open_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    auto params = TRY(copy_typed_from_user(user_params));

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

    auto path = TRY(get_syscall_path_argument(params.path));

    dbgln_if(IO_DEBUG, "sys$open(dirfd={}, path='{}', options={}, mode={})", dirfd, path->view(), options, mode);

    auto fd_allocation = TRY(m_fds.allocate());
    RefPtr<Custody> base;
    if (dirfd == AT_FDCWD) {
        base = current_directory();
    } else {
        auto base_description = TRY(fds().open_file_description(dirfd));
        if (!base_description->is_directory())
            return ENOTDIR;
        if (!base_description->custody())
            return EINVAL;
        base = base_description->custody();
    }

    auto description = TRY(VirtualFileSystem::the().open(path->view(), options, mode & ~umask(), *base));

    if (description->inode() && description->inode()->socket())
        return ENXIO;

    u32 fd_flags = (options & O_CLOEXEC) ? FD_CLOEXEC : 0;
    m_fds[fd_allocation.fd].set(move(description), fd_flags);
    return fd_allocation.fd;
}

ErrorOr<FlatPtr> Process::sys$close(int fd)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);
    auto description = TRY(fds().open_file_description(fd));
    auto result = description->close();
    m_fds[fd] = {};
    if (result.is_error())
        return result.release_error();
    return 0;
}

}
