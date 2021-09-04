/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$chdir(Userspace<char const*> user_path, size_t path_length)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    REQUIRE_PROMISE(rpath);
    auto path = get_syscall_path_argument(user_path, path_length);
    if (path.is_error())
        return path.error();
    auto directory_or_error = VirtualFileSystem::the().open_directory(path.value()->view(), current_directory());
    if (directory_or_error.is_error())
        return directory_or_error.error();
    m_cwd = *directory_or_error.value();
    return 0;
}

KResultOr<FlatPtr> Process::sys$fchdir(int fd)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    REQUIRE_PROMISE(stdio);
    auto description = fds().file_description(fd);
    if (!description)
        return EBADF;

    if (!description->is_directory())
        return ENOTDIR;

    if (!description->metadata().may_execute(*this))
        return EACCES;

    m_cwd = description->custody();
    return 0;
}

KResultOr<FlatPtr> Process::sys$getcwd(Userspace<char*> buffer, size_t size)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    REQUIRE_PROMISE(rpath);

    if (size > NumericLimits<ssize_t>::max())
        return EINVAL;

    auto maybe_path = current_directory().try_create_absolute_path();
    if (!maybe_path)
        return ENOMEM;
    auto& path = *maybe_path;

    size_t ideal_size = path.length() + 1;
    auto size_to_copy = min(ideal_size, size);
    if (!copy_to_user(buffer, path.characters(), size_to_copy))
        return EFAULT;
    // Note: we return the whole size here, not the copied size.
    return ideal_size;
}

}
