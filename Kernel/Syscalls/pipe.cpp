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
    auto fifo = FIFO::create(uid());

    auto open_reader_result = fifo->open_direction(FIFO::Direction::Reader);
    if (open_reader_result.is_error())
        return open_reader_result.error();
    auto open_writer_result = fifo->open_direction(FIFO::Direction::Writer);
    if (open_writer_result.is_error())
        return open_writer_result.error();

    auto reader_fd_or_error = m_fds.allocate();
    if (reader_fd_or_error.is_error())
        return reader_fd_or_error.error();
    auto reader_fd = reader_fd_or_error.release_value();
    m_fds[reader_fd.fd].set(open_reader_result.release_value(), fd_flags);
    m_fds[reader_fd.fd].description()->set_readable(true);
    if (!copy_to_user(&pipefd[0], &reader_fd.fd))
        return EFAULT;

    auto writer_fd_or_error = m_fds.allocate();
    if (writer_fd_or_error.is_error())
        return writer_fd_or_error.error();
    auto writer_fd = writer_fd_or_error.release_value();
    m_fds[writer_fd.fd].set(open_writer_result.release_value(), fd_flags);
    m_fds[writer_fd.fd].description()->set_writable(true);

    if (!copy_to_user(&pipefd[1], &writer_fd.fd))
        return EFAULT;

    return 0;
}

}
