/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$chroot(Userspace<const char*> user_path, size_t path_length, int mount_flags)
{
    if (!is_superuser())
        return EPERM;
    REQUIRE_PROMISE(chroot);
    auto path = get_syscall_path_argument(user_path, path_length);
    if (path.is_error())
        return path.error();
    auto directory_or_error = VFS::the().open_directory(path.value()->view(), current_directory());
    if (directory_or_error.is_error())
        return directory_or_error.error();
    auto directory = directory_or_error.value();
    m_root_directory_relative_to_global_root = directory;
    int chroot_mount_flags = mount_flags == -1 ? directory->mount_flags() : mount_flags;

    auto custody_or_error = Custody::try_create(nullptr, "", directory->inode(), chroot_mount_flags);
    if (custody_or_error.is_error())
        return custody_or_error.error();

    set_root_directory(custody_or_error.release_value());
    return 0;
}

}
