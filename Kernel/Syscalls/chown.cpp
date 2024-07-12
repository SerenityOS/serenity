/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/RefPtr.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$fchown(int fd, UserID uid, GroupID gid)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::chown));
    auto description = TRY(open_file_description(fd));
    TRY(description->chown(credentials(), uid, gid));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$chown(Userspace<Syscall::SC_chown_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::chown));
    auto params = TRY(copy_typed_from_user(user_params));
    auto path = TRY(get_syscall_path_argument(params.path));
    CustodyBase base(params.dirfd, path->view());
    TRY(VirtualFileSystem::chown(vfs_root_context(), credentials(), path->view(), params.uid, params.gid, base, params.follow_symlinks ? 0 : O_NOFOLLOW_NOERROR));
    return 0;
}

}
