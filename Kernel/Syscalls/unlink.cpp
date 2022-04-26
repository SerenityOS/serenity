/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$unlink(int dirfd, Userspace<char const*> user_path, size_t path_length, int flags)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this)
    TRY(require_promise(Pledge::cpath));
    auto path = TRY(get_syscall_path_argument(user_path, path_length));

    if (flags & ~AT_REMOVEDIR)
        return Error::from_errno(EINVAL);

    RefPtr<Custody> base;
    if (dirfd == AT_FDCWD) {
        base = current_directory();
    } else {
        auto base_description = TRY(open_file_description(dirfd));
        if (!base_description->custody())
            return Error::from_errno(EINVAL);
        base = base_description->custody();
    }

    if (flags & AT_REMOVEDIR)
        TRY(VirtualFileSystem::the().rmdir(path->view(), *base));
    else
        TRY(VirtualFileSystem::the().unlink(path->view(), *base));
    return 0;
}

}
