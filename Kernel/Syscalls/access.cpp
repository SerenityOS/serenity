/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$access(Userspace<const Syscall::SC_access_params*> user_params)
{
    REQUIRE_PROMISE(rpath);
    Syscall::SC_access_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;
    auto dirfd = get_syscall_fd_argument(params.dirfd);
    if (dirfd.is_error())
        return dirfd.error();
    auto path = get_syscall_path_argument(params.path);
    if (path.is_error())
        return path.error();
    return VFS::the().access({ *dirfd.value(), path.value()->view() }, params.mode, params.flags);
}

}
