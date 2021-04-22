/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<ssize_t> Process::sys$get_dir_entries(int fd, Userspace<void*> user_buffer, ssize_t user_size)
{
    REQUIRE_PROMISE(stdio);
    if (user_size < 0)
        return EINVAL;
    auto description = file_description(fd);
    if (!description)
        return EBADF;
    auto buffer = UserOrKernelBuffer::for_user_buffer(user_buffer, static_cast<size_t>(user_size));
    if (!buffer.has_value())
        return EFAULT;
    return description->get_dir_entries(buffer.value(), user_size);
}

}
