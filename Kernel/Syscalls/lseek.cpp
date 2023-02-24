/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$lseek(int fd, Userspace<off_t*> userspace_offset, int whence)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    auto description = TRY(open_file_description(fd));
    off_t offset;
    TRY(copy_from_user(&offset, userspace_offset));
    auto seek_result = TRY(description->seek(offset, whence));
    TRY(copy_to_user(userspace_offset, &seek_result));
    return 0;
}

}
