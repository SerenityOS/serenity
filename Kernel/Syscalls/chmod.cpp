/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$chmod(Userspace<Syscall::SC_chmod_params const*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::fattr));
    auto params = TRY(copy_typed_from_user(user_params));
    auto path = TRY(get_syscall_path_argument(params.path));

    RefPtr<Custody> base;
    if (params.dirfd == AT_FDCWD) {
        base = current_directory();
    } else {
        auto base_description = TRY(open_file_description(params.dirfd));
        if (!base_description->custody())
            return EINVAL;
        base = base_description->custody();
    }

    TRY(VirtualFileSystem::the().chmod(path->view(), params.mode, *base, params.follow_symlinks ? 0 : O_NOFOLLOW_NOERROR));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$fchmod(int fd, mode_t mode)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::fattr));
    auto description = TRY(open_file_description(fd));
    TRY(description->chmod(mode));
    return 0;
}

}
