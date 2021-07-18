/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$fcntl(int fd, int cmd, u32 arg)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    REQUIRE_PROMISE(stdio);
    dbgln_if(IO_DEBUG, "sys$fcntl: fd={}, cmd={}, arg={}", fd, cmd, arg);
    auto description = fds().file_description(fd);
    if (!description)
        return EBADF;
    // NOTE: The FD flags are not shared between FileDescription objects.
    //       This means that dup() doesn't copy the FD_CLOEXEC flag!
    switch (cmd) {
    case F_DUPFD: {
        int arg_fd = (int)arg;
        if (arg_fd < 0)
            return EINVAL;
        int new_fd = fds().allocate(arg_fd);
        if (new_fd < 0)
            return new_fd;
        m_fds[new_fd].set(*description);
        return new_fd;
    }
    case F_GETFD:
        return m_fds[fd].flags();
    case F_SETFD:
        m_fds[fd].set_flags(arg);
        break;
    case F_GETFL:
        return description->file_flags();
    case F_SETFL:
        description->set_file_flags(arg);
        break;
    case F_ISTTY:
        return description->is_tty();
    default:
        return EINVAL;
    }
    return 0;
}

}
