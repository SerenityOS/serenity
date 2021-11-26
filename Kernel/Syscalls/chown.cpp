/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$fchown(int fd, UserID uid, GroupID gid)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    REQUIRE_PROMISE(chown);
    auto description = TRY(fds().open_file_description(fd));
    TRY(description->chown(uid, gid));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$chown(Userspace<const Syscall::SC_chown_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    REQUIRE_PROMISE(chown);
    auto params = TRY(copy_typed_from_user(user_params));
    auto path = TRY(get_syscall_path_argument(params.path));
    TRY(VirtualFileSystem::the().chown(path->view(), params.uid, params.gid, current_directory()));
    return 0;
}

}
