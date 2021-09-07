/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$get_dir_entries(int fd, Userspace<void*> user_buffer, size_t user_size)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    REQUIRE_PROMISE(stdio);
    if (user_size > NumericLimits<ssize_t>::max())
        return EINVAL;
    auto description = TRY(fds().open_file_description(fd));
    auto buffer = UserOrKernelBuffer::for_user_buffer(user_buffer, static_cast<size_t>(user_size));
    if (!buffer.has_value())
        return EFAULT;
    auto result = description->get_dir_entries(buffer.value(), user_size);
    if (result.is_error())
        return result.error();
    else
        return result.release_value();
}

}
