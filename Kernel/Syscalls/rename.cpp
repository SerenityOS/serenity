/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$rename(Userspace<const Syscall::SC_rename_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(cpath);
    auto params = TRY(copy_typed_from_user(user_params));
    auto old_path = TRY(get_syscall_path_argument(params.old_path));
    auto new_path = TRY(get_syscall_path_argument(params.new_path));
    return VirtualFileSystem::the().rename(old_path->view(), new_path->view(), current_directory());
}

}
