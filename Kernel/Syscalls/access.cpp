/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$access(Userspace<Syscall::SC_access_params const*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::rpath));

    auto params = TRY(copy_typed_from_user(user_params));
    auto path = TRY(get_syscall_path_argument(params.path));

    if (params.flags & ~(AT_SYMLINK_NOFOLLOW | AT_EACCESS))
        return EINVAL;

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

    TRY(VirtualFileSystem::the().access(path->view(), params.mode, *base, params.flags));
    return 0;
}

}
