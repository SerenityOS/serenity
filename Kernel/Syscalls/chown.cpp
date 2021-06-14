/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$fchown(int fd, uid_t uid, gid_t gid)
{
    REQUIRE_PROMISE(chown);
    auto description = file_description(fd);
    if (!description)
        return EBADF;
    return description->chown(uid, gid);
}

KResultOr<int> Process::sys$chown(Userspace<const Syscall::SC_chown_params*> user_params)
{
    REQUIRE_PROMISE(chown);
    Syscall::SC_chown_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;
    auto dirfd = get_syscall_fd_argument(params.dirfd);
    if (dirfd.is_error())
        return dirfd.error();
    auto path = get_syscall_path_argument(params.path);
    if (path.is_error())
        return path.error();
    return VFS::the().chown({ *dirfd.value(), path.value()->view() }, params.uid, params.gid, params.flags);
}

}
