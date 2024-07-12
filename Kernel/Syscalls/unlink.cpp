/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$unlink(int dirfd, Userspace<char const*> user_path, size_t path_length, int flags)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::cpath));
    auto path = TRY(get_syscall_path_argument(user_path, path_length));

    if (flags & ~AT_REMOVEDIR)
        return Error::from_errno(EINVAL);

    CustodyBase base(dirfd, path->view());

    if (flags & AT_REMOVEDIR)
        TRY(VirtualFileSystem::rmdir(vfs_root_context(), credentials(), path->view(), base));
    else
        TRY(VirtualFileSystem::unlink(vfs_root_context(), credentials(), path->view(), base));
    return 0;
}

}
