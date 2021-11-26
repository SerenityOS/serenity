/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$chmod(Userspace<const char*> user_path, size_t path_length, mode_t mode)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    REQUIRE_PROMISE(fattr);
    auto path = TRY(get_syscall_path_argument(user_path, path_length));
    TRY(VirtualFileSystem::the().chmod(path->view(), mode, current_directory()));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$fchmod(int fd, mode_t mode)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    REQUIRE_PROMISE(fattr);
    auto description = TRY(fds().open_file_description(fd));
    TRY(description->chmod(mode));
    return 0;
}

}
