/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$link(Userspace<const Syscall::SC_link_params*> user_params)
{
    REQUIRE_PROMISE(cpath);
    Syscall::SC_link_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;
    auto old_path = copy_string_from_user(params.old_path);
    if (old_path.is_null())
        return EFAULT;
    auto new_path = copy_string_from_user(params.new_path);
    if (new_path.is_null())
        return EFAULT;
    return VFS::the().link(old_path, new_path, current_directory());
}

KResultOr<int> Process::sys$symlink(Userspace<const Syscall::SC_symlink_params*> user_params)
{
    REQUIRE_PROMISE(cpath);
    Syscall::SC_symlink_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;
    auto target = get_syscall_path_argument(params.target);
    if (target.is_error())
        return target.error();
    auto linkpath = get_syscall_path_argument(params.linkpath);
    if (linkpath.is_error())
        return linkpath.error();
    return VFS::the().symlink(target.value()->view(), linkpath.value()->view(), current_directory());
}

}
