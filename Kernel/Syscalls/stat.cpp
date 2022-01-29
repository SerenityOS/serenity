/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullRefPtrVector.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$fstat(int fd, Userspace<stat*> user_statbuf)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::stdio));
    auto description = TRY(open_file_description(fd));
    auto buffer = TRY(description->stat());
    TRY(copy_to_user(user_statbuf, &buffer));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$stat(Userspace<const Syscall::SC_stat_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::rpath));
    auto params = TRY(copy_typed_from_user(user_params));

    auto path = TRY(get_syscall_path_argument(params.path));

    RefPtr<Custody> base;
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
