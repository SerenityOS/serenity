/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$link(Userspace<Syscall::SC_link_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::cpath));
    auto params = TRY(copy_typed_from_user(user_params));
    auto old_path = TRY(try_copy_kstring_from_user(params.old_path));
    auto new_path = TRY(try_copy_kstring_from_user(params.new_path));

    TRY(VirtualFileSystem::link(vfs_root_context(), credentials(), old_path->view(), new_path->view(), current_directory()));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$symlink(Userspace<Syscall::SC_symlink_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::cpath));
    auto params = TRY(copy_typed_from_user(user_params));

    auto target = TRY(get_syscall_path_argument(params.target));
    auto linkpath = TRY(get_syscall_path_argument(params.linkpath));
    CustodyBase base(params.dirfd, target->view());
    TRY(VirtualFileSystem::symlink(vfs_root_context(), credentials(), target->view(), linkpath->view(), base));
    return 0;
}

}
