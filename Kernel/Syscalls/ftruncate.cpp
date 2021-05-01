/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$ftruncate(int fd, Userspace<off_t*> userspace_length)
{
    REQUIRE_PROMISE(stdio);
    off_t length;
    if (!copy_from_user(&length, userspace_length))
        return EFAULT;
    if (length < 0)
        return EINVAL;
    auto description = file_description(fd);
    if (!description)
        return EBADF;
    if (!description->is_writable())
        return EBADF;
    return description->truncate(static_cast<u64>(length));
}

}
