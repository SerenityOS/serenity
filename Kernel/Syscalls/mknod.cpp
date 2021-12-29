/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$mknod(Userspace<const Syscall::SC_mknod_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::dpath));
    auto params = TRY(copy_typed_from_user(user_params));

    if (!is_superuser() && !is_regular_file(params.mode) && !is_fifo(params.mode) && !is_socket(params.mode))
        return EPERM;
    auto path = TRY(get_syscall_path_argument(params.path));
    TRY(VirtualFileSystem::the().mknod(path->view(), params.mode & ~umask(), params.dev, current_directory()));
    return 0;
}

}
