/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$mknod(Userspace<const Syscall::SC_mknod_params*> user_params)
{
    REQUIRE_PROMISE(dpath);
    Syscall::SC_mknod_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;
    if (!is_superuser() && !is_regular_file(params.mode) && !is_fifo(params.mode) && !is_socket(params.mode))
        return EPERM;
    auto path = get_syscall_path_argument(params.path);
    if (path.is_error())
        return path.error();
    return VFS::the().mknod(path.value()->view(), params.mode & ~umask(), params.dev, current_directory());
}

}
