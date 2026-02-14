/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$realpath(Userspace<Syscall::SC_realpath_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::rpath));
    auto params = TRY(copy_typed_from_user(user_params));

    auto user_path = TRY(get_syscall_path_argument(params.path));

    UnresolvedPath unresolved_path(current_directory().custody(), user_path->view());
    auto canonicalized_path = TRY(VirtualFileSystem::optimize_path(unresolved_path));
    auto real_path = TRY(VirtualFileSystem::resolve_path(vfs_root_context(), credentials(), *canonicalized_path));
    auto absolute_path = real_path->optimized_path().full_canonicalized_path();

    size_t ideal_size = absolute_path.length() + 1;
    auto size_to_copy = min(ideal_size, params.buffer.size);
    TRY(copy_to_user(params.buffer.data, absolute_path.characters_without_null_termination(), size_to_copy));
    // Note: we return the whole size here, not the copied size.
    return ideal_size;
}

}
