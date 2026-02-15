/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Library/StdLib.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$readlink(Userspace<Syscall::SC_readlink_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::rpath));
    auto params = TRY(copy_typed_from_user(user_params));

    auto path = TRY(get_syscall_path_argument(params.path));
    auto description = TRY(VirtualFileSystem::open(vfs_root_context(), credentials(), path->view(), O_RDONLY | O_NOFOLLOW_NOERROR, 0, TRY(custody_for_dirfd(params.dirfd))));

    if (!description->metadata().is_symlink())
        return EINVAL;

    // Make sure that our assumptions about the path length hold up.
    // Note that this doesn't mean that the reported size can be trusted, some inodes just report zero.
    VERIFY(description->inode()->size() <= MAXPATHLEN);

    auto link_target = TRY(description->inode()->read_as_link(description));
    auto size_to_copy = min(link_target.stored_length(), params.buffer.size);

    TRY(copy_to_user(params.buffer.data, link_target.storage().data(), size_to_copy));
    // Note: we return the whole size here, not the copied size.
    return link_target.stored_length();
}

}
