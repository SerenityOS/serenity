/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$chdir(Userspace<const char*> user_path, size_t path_length)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::rpath));
    auto path = TRY(get_syscall_path_argument(user_path, path_length));
    m_cwd = TRY(VirtualFileSystem::the().open_directory(path->view(), current_directory()));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$fchdir(int fd)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::stdio));
    auto description = TRY(open_file_description(fd));
    if (!description->is_directory())
        return ENOTDIR;
    if (!description->metadata().may_execute(*this))
        return EACCES;
    m_cwd = description->custody();
    return 0;
}

ErrorOr<FlatPtr> Process::sys$getcwd(Userspace<char*> buffer, size_t size)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::rpath));

    if (size > NumericLimits<ssize_t>::max())
        return EINVAL;

    auto path = TRY(current_directory().try_serialize_absolute_path());
    size_t ideal_size = path->length() + 1;
    auto size_to_copy = min(ideal_size, size);
    TRY(copy_to_user(buffer, path->characters(), size_to_copy));
    // Note: we return the whole size here, not the copied size.
    return ideal_size;
}

}
