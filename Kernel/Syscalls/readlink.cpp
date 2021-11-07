/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$readlink(Userspace<const Syscall::SC_readlink_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(rpath);
    auto params = TRY(copy_typed_from_user(user_params));

    auto path = TRY(get_syscall_path_argument(params.path));
    auto description = TRY(VirtualFileSystem::the().open(path->view(), O_RDONLY | O_NOFOLLOW_NOERROR, 0, current_directory()));

    if (!description->metadata().is_symlink())
        return EINVAL;

    auto link_target = TRY(description->read_entire_file());
    auto size_to_copy = min(link_target->size(), params.buffer.size);
    TRY(copy_to_user(params.buffer.data, link_target->data(), size_to_copy));
    // Note: we return the whole size here, not the copied size.
    return link_target->size();
}

}
