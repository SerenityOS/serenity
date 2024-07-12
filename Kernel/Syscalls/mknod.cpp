/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$mknod(Userspace<Syscall::SC_mknod_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::dpath));
    auto params = TRY(copy_typed_from_user(user_params));

    auto credentials = this->credentials();
    if (!credentials->is_superuser() && !is_regular_file(params.mode) && !is_fifo(params.mode) && !is_socket(params.mode))
        return EPERM;
    auto path = TRY(get_syscall_path_argument(params.path));

    CustodyBase base(params.dirfd, path->view());
    TRY(VirtualFileSystem::mknod(vfs_root_context(), credentials, path->view(), params.mode & ~umask(), params.dev, base));
    return 0;
}

}
