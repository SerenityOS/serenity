/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

// TODO: The length could be passed by value instead of a pointer
ErrorOr<FlatPtr> Process::sys$ftruncate(int fd, Userspace<off_t const*> userspace_length)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    auto length = TRY(copy_typed_from_user(userspace_length));
    if (length < 0)
        return EINVAL;
    auto description = TRY(open_file_description(fd));
    if (!description->is_writable())
        return EBADF;
    TRY(description->truncate(static_cast<u64>(length)));
    return 0;
}

}
