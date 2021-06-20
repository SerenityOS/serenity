/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$unlink(Userspace<const Syscall::SC_unlink_params*> user_params)
{
    REQUIRE_PROMISE(cpath);
    Syscall::SC_unlink_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;
    auto dirfd = get_syscall_fd_argument(params.dirfd);
    if (dirfd.is_error())
        return dirfd.error();
    auto path = get_syscall_path_argument(params.path);
    if (path.is_error())
        return path.error();
    return VFS::the().unlink({ *dirfd.value(), path.value()->view() }, params.flags);
}

}
