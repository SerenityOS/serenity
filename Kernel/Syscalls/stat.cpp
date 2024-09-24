/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/RefPtr.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Tasks/Process.h>

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

    CustodyBase base(params.dirfd, path->view());
    auto metadata = TRY(VirtualFileSystem::lookup_metadata(vfs_root_context(), credentials(), path->view(), base, params.follow_symlinks ? 0 : O_NOFOLLOW_NOERROR));
    auto statbuf = TRY(metadata.stat());
    TRY(copy_to_user(params.statbuf, &statbuf));
    return 0;
}

}
