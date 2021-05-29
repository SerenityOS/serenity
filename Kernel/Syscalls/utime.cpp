/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$utime(Userspace<const char*> user_path, size_t path_length, Userspace<const struct utimbuf*> user_buf)
{
    REQUIRE_PROMISE(fattr);
    auto path = get_syscall_path_argument(user_path, path_length);
    if (path.is_error())
        return path.error();
    utimbuf buf;
    if (user_buf) {
        if (!copy_from_user(&buf, user_buf))
            return EFAULT;
    } else {
        auto now = kgettimeofday().to_truncated_seconds();
        // Not a bug!
        buf = { now, now };
    }
    return VFS::the().utime(path.value()->view(), current_directory(), buf.actime, buf.modtime);
}

}
