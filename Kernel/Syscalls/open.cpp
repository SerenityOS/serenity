/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/RefPtr.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Library/KLexicalPath.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::open_impl(Userspace<Syscall::SC_open_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    auto params = TRY(copy_typed_from_user(user_params));

    int dirfd = params.dirfd;
    int options = params.options;
    u16 mode = params.mode;

    if (options & O_NOFOLLOW_NOERROR)
        return EINVAL;

    if (options & O_UNLINK_INTERNAL)
        return EINVAL;

    auto path = TRY(get_syscall_path_argument(params.path));

    // Disable checking open pledges when building userspace with coverage
    // so that all processes can write out coverage data even with pledges
    bool skip_pledge_verification = false;

#ifdef SKIP_PATH_VALIDATION_FOR_COVERAGE_INSTRUMENTATION
    if (KLexicalPath::basename(path->view()).ends_with(".profraw"sv))
        skip_pledge_verification = true;
#endif
    if (!skip_pledge_verification) {
        if (options & O_WRONLY)
            TRY(require_promise(Pledge::wpath));
        else if (options & O_RDONLY)
            TRY(require_promise(Pledge::rpath));

        if (options & O_CREAT)
            TRY(require_promise(Pledge::cpath));
    }

    // Ignore everything except permission bits.
    mode &= 0777;

    dbgln_if(IO_DEBUG, "sys$open(dirfd={}, path='{}', options={}, mode={})", dirfd, path->view(), options, mode);

    auto fd_allocation = TRY(allocate_fd());
    CustodyBase base(dirfd, path->view());
    auto description = TRY(VirtualFileSystem::open(vfs_root_context(), credentials(), path->view(), options, mode & ~umask(), base));

    if (description->inode() && description->inode()->bound_socket())
        return ENXIO;

    return m_fds.with_exclusive([&](auto& fds) -> ErrorOr<FlatPtr> {
        u32 fd_flags = (options & O_CLOEXEC) ? FD_CLOEXEC : 0;
        fds[fd_allocation.fd].set(move(description), fd_flags);
        return fd_allocation.fd;
    });
}

ErrorOr<FlatPtr> Process::close_impl(int fd)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    auto description = TRY(open_file_description(fd));
    auto result = description->close();
    m_fds.with_exclusive([fd](auto& fds) { fds[fd] = {}; });
    if (result.is_error())
        return result.release_error();
    return 0;
}

}
