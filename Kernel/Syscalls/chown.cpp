/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$fchown(int fd, UserID uid, GroupID gid)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    REQUIRE_PROMISE(chown);
    auto description = TRY(fds().file_description(fd));
    return description->chown(uid, gid);
}

KResultOr<FlatPtr> Process::sys$chown(Userspace<const Syscall::SC_chown_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    REQUIRE_PROMISE(chown);
    auto params = TRY(copy_typed_from_user(user_params));

    auto path = TRY(get_syscall_path_argument(params.path));
    return VirtualFileSystem::the().chown(path->view(), params.uid, params.gid, current_directory());
}

}
