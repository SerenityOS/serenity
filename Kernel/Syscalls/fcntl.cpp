/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$fcntl(int fd, int cmd, u32 arg)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    REQUIRE_PROMISE(stdio);
    dbgln_if(IO_DEBUG, "sys$fcntl: fd={}, cmd={}, arg={}", fd, cmd, arg);
    auto description = TRY(fds().open_file_description(fd));
    // NOTE: The FD flags are not shared between OpenFileDescription objects.
    //       This means that dup() doesn't copy the FD_CLOEXEC flag!
    switch (cmd) {
    case F_DUPFD: {
        int arg_fd = (int)arg;
        if (arg_fd < 0)
            return EINVAL;
        auto fd_allocation = TRY(m_fds.allocate(arg_fd));
        m_fds[fd_allocation.fd].set(*description);
        return fd_allocation.fd;
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
    case F_GETLK:
        TRY(description->get_flock(Userspace<flock*>(arg)));
        return 0;
    case F_SETLK:
        TRY(description->apply_flock(Process::current(), Userspace<const flock*>(arg)));
        return 0;
    default:
        return EINVAL;
    }
    return 0;
}

}
