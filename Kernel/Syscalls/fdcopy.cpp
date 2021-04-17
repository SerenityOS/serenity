/*
 * Copyright (c) 2021, Leandro Pereira <leandro@tia.mat.br>
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

#include <AK/StdLibExtras.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

using BlockFlags = Thread::FileBlocker::BlockFlags;

KResultOr<ssize_t> Process::sys$fdcopy(int srcfd, int dstfd, ssize_t count)
{
    REQUIRE_PROMISE(stdio);

    if (count > (ssize_t)MiB)
        count = MiB;
    if (count < 0)
        return EINVAL;

    auto source_description = file_description(srcfd);
    if (!source_description)
        return EBADF;
    if (!source_description->is_readable())
        return EBADF;
    if (source_description->is_directory())
        return EISDIR;

    auto destination_description = file_description(dstfd);
    if (!destination_description)
        return EBADF;
    if (!destination_description->is_writable())
        return EBADF;
    if (destination_description->is_directory())
        return EISDIR;

    auto backing_buffer = KBuffer::try_create_with_size(AK::min((ssize_t)65536, count));
    if (!backing_buffer)
        return ENOMEM;

    auto buffer = UserOrKernelBuffer::for_kernel_buffer(backing_buffer->data());

    ssize_t bytes_copied = 0;

    while (count) {
        if (source_description->is_blocking() && !source_description->can_read()) {
            auto unblock_flags = BlockFlags::None;
            if (Thread::current()->block<Thread::ReadBlocker>({}, *source_description, unblock_flags).was_interrupted())
                return EINTR;
            if (!has_flag(unblock_flags, BlockFlags::Read))
                return EAGAIN;
        }

        auto bytes_to_read = AK::min(backing_buffer->capacity(), (size_t)count);
        auto read_result = source_description->read(buffer, bytes_to_read);
        if (read_result.is_error())
            return read_result.error();
        if (!read_result.value())
            break;

        auto write_result = do_write(*destination_description, buffer, read_result.value());
        if (write_result.is_error())
            return write_result.error();

        bytes_copied += (ssize_t)write_result.value();
        count -= write_result.value();
    }

    return bytes_copied;
}

}
