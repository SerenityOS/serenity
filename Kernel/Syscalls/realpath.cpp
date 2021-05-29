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

KResultOr<int> Process::sys$realpath(Userspace<const Syscall::SC_realpath_params*> user_params)
{
    REQUIRE_PROMISE(rpath);

    Syscall::SC_realpath_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;

    auto path = get_syscall_path_argument(params.path);
    if (path.is_error())
        return path.error();

    auto custody_or_error = VFS::the().resolve_path(path.value()->view(), current_directory());
    if (custody_or_error.is_error())
        return custody_or_error.error();
    auto& custody = custody_or_error.value();
    auto absolute_path = custody->absolute_path();

    size_t ideal_size = absolute_path.length() + 1;
    auto size_to_copy = min(ideal_size, params.buffer.size);
    if (!copy_to_user(params.buffer.data, absolute_path.characters(), size_to_copy))
        return EFAULT;
    // Note: we return the whole size here, not the copied size.
    return ideal_size;
};

}
