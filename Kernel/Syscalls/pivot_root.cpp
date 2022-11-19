/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$pivot_root(Userspace<Syscall::SC_pivot_root_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    auto params = TRY(copy_typed_from_user(user_params));
    int dirfd = params.dirfd;
    auto description = TRY(open_file_description(dirfd));
    if (!description->custody()) {
        return EINVAL;
    }
    TRY(VirtualFileSystem::the().pivot_root(*description->custody()));
    return 0;
}

}
