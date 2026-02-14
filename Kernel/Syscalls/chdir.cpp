/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/RefPtr.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$chdir(Userspace<char const*> user_path, size_t path_length)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::rpath));
    auto path = TRY(get_syscall_path_argument(user_path, path_length));
    UnresolvedPath unresolved_path(current_directory().custody(), path->view());
    auto new_directory = TRY(VirtualFileSystem::open_directory(vfs_root_context(), credentials(), unresolved_path));
    TRY(m_current_directory.with([&](auto& current_directory) -> ErrorOr<void> {
        current_directory = *new_directory;
        return {};
    }));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$fchdir(int fd)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    auto description = TRY(open_file_description(fd));
    if (!description->is_directory())
        return ENOTDIR;
    if (!description->metadata().may_execute(credentials()))
        return EACCES;

    TRY(m_current_directory.with([&](auto& current_directory) -> ErrorOr<void> {
        return description->m_state.with([&](auto& state) -> ErrorOr<void> {
            current_directory = *state.path;
            return {};
        });
    }));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$getcwd(Userspace<char*> buffer, size_t size)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::rpath));

    if (size > NumericLimits<ssize_t>::max())
        return EINVAL;

    auto path = TRY(current_directory().custody().try_serialize_absolute_path());
    size_t ideal_size = path->length() + 1;
    auto size_to_copy = min(ideal_size, size);
    TRY(copy_to_user(buffer, path->characters(), size_to_copy));
    // Note: we return the whole size here, not the copied size.
    return ideal_size;
}

}
