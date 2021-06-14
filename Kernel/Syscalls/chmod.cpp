/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$chmod(Userspace<const Syscall::SC_chmod_params*> user_params)
{
    REQUIRE_PROMISE(fattr);
    Syscall::SC_chmod_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;
    auto dirfd = get_syscall_fd_argument(params.dirfd);
    if (dirfd.is_error())
        return dirfd.error();
    auto path = get_syscall_path_argument(params.path);
    if (path.is_error())
        return path.error();
    return VFS::the().chmod({ *dirfd.value(), path.value()->view() }, params.mode, params.flags);
}

KResultOr<int> Process::sys$fchmod(int fd, mode_t mode)
{
    REQUIRE_PROMISE(fattr);
    auto description = file_description(fd);
    if (!description)
        return EBADF;
    return description->chmod(mode);
}

}
