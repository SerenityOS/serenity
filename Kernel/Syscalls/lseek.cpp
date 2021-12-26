/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$lseek(int fd, Userspace<off_t*> userspace_offset, int whence)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);
    auto description = fds().file_description(fd);
    if (!description)
        return EBADF;
    off_t offset;
    TRY(copy_from_user(&offset, userspace_offset));
    auto seek_result = TRY(description->seek(offset, whence));
    return copy_to_user(userspace_offset, &seek_result);
}

}
