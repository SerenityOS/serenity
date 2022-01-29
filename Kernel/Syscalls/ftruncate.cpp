/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

// NOTE: The length is passed by pointer because off_t is 64bit,
// hence it can't be passed by register on 32bit platforms.
ErrorOr<FlatPtr> Process::sys$ftruncate(int fd, Userspace<off_t const*> userspace_length)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
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
