/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/FIFO.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$pipe(Userspace<int*> pipefd, int flags)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));

    // Reject flags other than O_CLOEXEC, O_NONBLOCK
    if ((flags & (O_CLOEXEC | O_NONBLOCK)) != flags)
        return EINVAL;

    u32 fd_flags = (flags & O_CLOEXEC) ? FD_CLOEXEC : 0;
    auto credentials = this->credentials();
    auto fifo = TRY(FIFO::try_create(credentials->uid()));

    auto reader_description = TRY(fifo->open_direction(FIFO::Direction::Reader));
    auto writer_description = TRY(fifo->open_direction(FIFO::Direction::Writer));

    reader_description->set_readable(true);
    writer_description->set_writable(true);
    if (flags & O_NONBLOCK) {
        reader_description->set_blocking(false);
        writer_description->set_blocking(false);
    }

    TRY(m_fds.with_exclusive([&](auto& fds) -> ErrorOr<void> {
        auto reader_fd_allocation = TRY(fds.allocate());
        auto writer_fd_allocation = TRY(fds.allocate());

        fds[reader_fd_allocation.fd].set(move(reader_description), fd_flags);
        fds[writer_fd_allocation.fd].set(move(writer_description), fd_flags);

        int fds_for_userspace[2] = {
            reader_fd_allocation.fd,
            writer_fd_allocation.fd,
        };
        if (copy_n_to_user(pipefd, fds_for_userspace, 2).is_error()) {
            // Avoid leaking both file descriptors on error.
            fds[reader_fd_allocation.fd] = {};
            fds[writer_fd_allocation.fd] = {};
            return EFAULT;
        }
        return {};
    }));
    return 0;
}

}
