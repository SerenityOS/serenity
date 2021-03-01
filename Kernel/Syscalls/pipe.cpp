/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    auto fifo = FIFO::create(m_uid);

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
