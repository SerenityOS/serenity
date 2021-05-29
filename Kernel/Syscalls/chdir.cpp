/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$chdir(Userspace<const char*> user_path, size_t path_length)
{
    REQUIRE_PROMISE(rpath);
    auto path = get_syscall_path_argument(user_path, path_length);
    if (path.is_error())
        return path.error();
    auto directory_or_error = VFS::the().open_directory(path.value()->view(), current_directory());
    if (directory_or_error.is_error())
        return directory_or_error.error();
    m_cwd = *directory_or_error.value();
    return 0;
}

KResultOr<int> Process::sys$fchdir(int fd)
{
    REQUIRE_PROMISE(stdio);
    auto description = file_description(fd);
    if (!description)
        return EBADF;

    if (!description->is_directory())
        return ENOTDIR;

    if (!description->metadata().may_execute(*this))
        return EACCES;

    m_cwd = description->custody();
    return 0;
}

KResultOr<int> Process::sys$getcwd(Userspace<char*> buffer, size_t size)
{
    REQUIRE_PROMISE(rpath);

    auto path = current_directory().absolute_path();

    size_t ideal_size = path.length() + 1;
    auto size_to_copy = min(ideal_size, size);
    if (!copy_to_user(buffer, path.characters(), size_to_copy))
        return EFAULT;
    // Note: we return the whole size here, not the copied size.
    return ideal_size;
}

}
