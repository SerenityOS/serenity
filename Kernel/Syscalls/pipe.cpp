/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/FIFO.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$pipe(int pipefd[2], int flags)
{
    REQUIRE_PROMISE(stdio);
    if (number_of_open_file_descriptors() + 2 > max_open_file_descriptors())
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

    int reader_fd = alloc_fd();
    m_fds[reader_fd].set(open_reader_result.release_value(), fd_flags);
    m_fds[reader_fd].description()->set_readable(true);
    if (!copy_to_user(&pipefd[0], &reader_fd))
        return EFAULT;

    int writer_fd = alloc_fd();
    m_fds[writer_fd].set(open_writer_result.release_value(), fd_flags);
    m_fds[writer_fd].description()->set_writable(true);
    if (!copy_to_user(&pipefd[1], &writer_fd))
        return EFAULT;

    return 0;
}

}
