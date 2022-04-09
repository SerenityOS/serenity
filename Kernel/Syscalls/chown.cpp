/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullRefPtrVector.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$fchown(int fd, UserID uid, GroupID gid)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::chown));
    auto description = TRY(open_file_description(fd));
    TRY(description->chown(uid, gid));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$chown(Userspace<Syscall::SC_chown_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::chown));
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

    TRY(VirtualFileSystem::the().chown(path->view(), params.uid, params.gid, *base, params.follow_symlinks ? 0 : O_NOFOLLOW_NOERROR));
    return 0;
}

}
