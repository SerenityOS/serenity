/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Library/NonnullLockRefPtrVector.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$fstat(int fd, Userspace<stat*> user_statbuf)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    auto description = TRY(open_file_description(fd));
    auto buffer = TRY(description->stat());
    TRY(copy_to_user(user_statbuf, &buffer));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$stat(Userspace<Syscall::SC_stat_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::rpath));
    auto params = TRY(copy_typed_from_user(user_params));

    auto path = TRY(get_syscall_path_argument(params.path));

    LockRefPtr<Custody> base;
    if (params.dirfd == AT_FDCWD) {
        base = current_directory();
    } else {
        auto base_description = TRY(open_file_description(params.dirfd));
        if (!base_description->is_directory())
            return ENOTDIR;
        if (!base_description->custody())
            return EINVAL;
        base = base_description->custody();
    }
    auto metadata = TRY(VirtualFileSystem::the().lookup_metadata(path->view(), *base, params.follow_symlinks ? 0 : O_NOFOLLOW_NOERROR));
    auto statbuf = TRY(metadata.stat());
    TRY(copy_to_user(params.statbuf, &statbuf));
    return 0;
}

}
