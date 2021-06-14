/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$rename(Userspace<const Syscall::SC_rename_params*> user_params)
{
    REQUIRE_PROMISE(cpath);
    Syscall::SC_rename_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;
    auto old_dirfd = get_syscall_fd_argument(params.old_dirfd);
    if (old_dirfd.is_error())
        return old_dirfd.error();
    auto old_path = get_syscall_path_argument(params.old_path);
    if (old_path.is_error())
        return old_path.error();
    auto new_dirfd = get_syscall_fd_argument(params.new_dirfd);
    if (new_dirfd.is_error())
        return new_dirfd.error();
    auto new_path = get_syscall_path_argument(params.new_path);
    if (new_path.is_error())
        return new_path.error();
    return VFS::the().rename({ *old_dirfd.value(), old_path.value()->view() }, { *new_dirfd.value(), new_path.value()->view() });
}

}
