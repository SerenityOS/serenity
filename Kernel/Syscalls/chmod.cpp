/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$chmod(Userspace<Syscall::SC_chmod_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::fattr));
    auto params = TRY(copy_typed_from_user(user_params));
    auto path = TRY(get_syscall_path_argument(params.path));
    CustodyBase base(params.dirfd, path->view());
    TRY(VirtualFileSystem::chmod(vfs_root_context(), credentials(), path->view(), params.mode, base, params.follow_symlinks ? 0 : O_NOFOLLOW_NOERROR));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$fchmod(int fd, mode_t mode)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::fattr));
    auto description = TRY(open_file_description(fd));
    TRY(description->chmod(credentials(), mode));
    return 0;
}

}
