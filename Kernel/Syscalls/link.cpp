/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$link(Userspace<const Syscall::SC_link_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(cpath);
    Syscall::SC_link_params params;
    TRY(copy_from_user(&params, user_params));
    auto old_path = TRY(try_copy_kstring_from_user(params.old_path));
    auto new_path = TRY(try_copy_kstring_from_user(params.new_path));
    return VirtualFileSystem::the().link(old_path->view(), new_path->view(), current_directory());
}

KResultOr<FlatPtr> Process::sys$symlink(Userspace<const Syscall::SC_symlink_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(cpath);
    Syscall::SC_symlink_params params;
    TRY(copy_from_user(&params, user_params));
    auto target = TRY(get_syscall_path_argument(params.target));
    auto linkpath = TRY(get_syscall_path_argument(params.linkpath));
    return VirtualFileSystem::the().symlink(target->view(), linkpath->view(), current_directory());
}

}
