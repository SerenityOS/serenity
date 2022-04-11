/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$rmdir(Userspace<char const*> user_path, size_t path_length)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this)
    TRY(require_promise(Pledge::cpath));
    auto path = TRY(get_syscall_path_argument(user_path, path_length));
    TRY(VirtualFileSystem::the().rmdir(path->view(), current_directory()));
    return 0;
}

}
