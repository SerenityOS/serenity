/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$fcntl(int fd, int cmd, uintptr_t arg)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    dbgln_if(IO_DEBUG, "sys$fcntl: fd={}, cmd={}, arg={}", fd, cmd, arg);
    auto description = TRY(open_file_description(fd));
    // NOTE: The FD flags are not shared between OpenFileDescription objects.
    //       This means that dup() doesn't copy the FD_CLOEXEC flag!
    switch (cmd) {
    case F_DUPFD_CLOEXEC:
    case F_DUPFD: {
        int arg_fd = (int)arg;
        if (arg_fd < 0)
            return EINVAL;
        return m_fds.with_exclusive([&](auto& fds) -> ErrorOr<FlatPtr> {
            auto fd_allocation = TRY(fds.allocate(arg_fd));
            fds[fd_allocation.fd].set(*description, (cmd == F_DUPFD_CLOEXEC) ? FD_CLOEXEC : 0);
            return fd_allocation.fd;
        });
    }
    case F_GETFD:
        return m_fds.with_exclusive([fd](auto& fds) { return fds[fd].flags(); });
    case F_SETFD:
        m_fds.with_exclusive([fd, arg](auto& fds) { fds[fd].set_flags(arg); });
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
        TRY(description->apply_flock(Process::current(), Userspace<flock const*>(arg), ShouldBlock::No));
        return 0;
    case F_SETLKW:
        TRY(description->apply_flock(Process::current(), Userspace<flock const*>(arg), ShouldBlock::Yes));
        return 0;
    default:
        return EINVAL;
    }
    return 0;
}

}
