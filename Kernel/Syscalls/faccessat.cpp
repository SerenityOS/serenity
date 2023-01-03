/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$faccessat(Userspace<Syscall::SC_faccessat_params const*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::rpath));
    auto params = TRY(copy_typed_from_user(user_params));
    auto pathname = TRY(get_syscall_path_argument(params.pathname));

    if ((params.flags & ~(AT_SYMLINK_NOFOLLOW | AT_EACCESS)) != 0)
        return EINVAL;

    auto flags = AccessFlags::None;
    if (params.flags & AT_SYMLINK_NOFOLLOW)
        flags |= AccessFlags::DoNotFollowSymlinks;
    if (params.flags & AT_EACCESS)
        flags |= AccessFlags::EffectiveAccess;

    TRY(VirtualFileSystem::the().access(credentials(), pathname->view(), params.mode, TRY(custody_for_dirfd(params.dirfd)), flags));
    return 0;
}

}
