/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$rename(Userspace<Syscall::SC_rename_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::cpath));
    auto params = TRY(copy_typed_from_user(user_params));
    auto old_path = TRY(get_syscall_path_argument(params.old_path));
    auto new_path = TRY(get_syscall_path_argument(params.new_path));
    CustodyBase old_base(params.olddirfd, old_path->view());
    CustodyBase new_base(params.newdirfd, new_path->view());
    TRY(VirtualFileSystem::rename(vfs_root_context(), credentials(), old_base, old_path->view(), new_base, new_path->view()));
    return 0;
}

}
