/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/FIFO.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$pipe(int pipefd[2], int flags)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);
    if (fds().open_count() + 2 > fds().max_open())
        return EMFILE;
    // Reject flags other than O_CLOEXEC.
    if ((flags & O_CLOEXEC) != flags)
        return EINVAL;

    u32 fd_flags = (flags & O_CLOEXEC) ? FD_CLOEXEC : 0;
    auto fifo = FIFO::try_create(uid());
    if (!fifo)
        return ENOMEM;

    auto reader_fd_allocation = TRY(m_fds.allocate());
    auto writer_fd_allocation = TRY(m_fds.allocate());

    auto reader_description = TRY(fifo->open_direction(FIFO::Direction::Reader));
    auto writer_description = TRY(fifo->open_direction(FIFO::Direction::Writer));

    reader_description->set_readable(true);
    writer_description->set_writable(true);

    m_fds[reader_fd_allocation.fd].set(move(reader_description), fd_flags);
    m_fds[writer_fd_allocation.fd].set(move(writer_description), fd_flags);

    if (!copy_to_user(&pipefd[0], &reader_fd_allocation.fd))
        return EFAULT;
    if (!copy_to_user(&pipefd[1], &writer_fd_allocation.fd))
        return EFAULT;

    return 0;
}

}
