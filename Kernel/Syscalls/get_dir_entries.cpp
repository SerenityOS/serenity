/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$get_dir_entries(int fd, Userspace<void*> user_buffer, size_t user_size)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    if (user_size > NumericLimits<ssize_t>::max())
        return EINVAL;
    auto description = TRY(open_file_description(fd));
    auto buffer = TRY(UserOrKernelBuffer::for_user_buffer(user_buffer, static_cast<size_t>(user_size)));
    auto count = TRY(description->get_dir_entries(buffer, user_size));
    return count;
}

}
