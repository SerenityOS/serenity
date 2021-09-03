/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$mknod(Userspace<const Syscall::SC_mknod_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(dpath);
    Syscall::SC_mknod_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;
    if (!is_superuser() && !is_regular_file(params.mode) && !is_fifo(params.mode) && !is_socket(params.mode))
        return EPERM;
    auto path = get_syscall_path_argument(params.path);
    if (path.is_error())
        return path.error();
    return VirtualFileSystem::the().mknod(path.value()->view(), params.mode & ~umask(), params.dev, current_directory());
}

}
