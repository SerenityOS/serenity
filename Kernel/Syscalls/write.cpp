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

#include <AK/NumericLimits.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

ssize_t Process::sys$writev(int fd, Userspace<const struct iovec*> iov, int iov_count)
{
    REQUIRE_PROMISE(stdio);
    if (iov_count < 0)
        return -EINVAL;

    {
        Checked checked_iov_count = sizeof(iovec);
        checked_iov_count *= iov_count;
        if (checked_iov_count.has_overflow())
            return -EFAULT;
    }

    u64 total_length = 0;
    Vector<iovec, 32> vecs;
    vecs.resize(iov_count);
    if (!copy_n_from_user(vecs.data(), iov, iov_count))
        return -EFAULT;
    for (auto& vec : vecs) {
        total_length += vec.iov_len;
        if (total_length > NumericLimits<i32>::max())
            return -EINVAL;
    }

    auto description = file_description(fd);
    if (!description)
        return -EBADF;

    if (!description->is_writable())
        return -EBADF;

    int nwritten = 0;
    for (auto& vec : vecs) {
        auto buffer = UserOrKernelBuffer::for_user_buffer((u8*)vec.iov_base, vec.iov_len);
        if (!buffer.has_value())
            return -EFAULT;
        int rc = do_write(*description, buffer.value(), vec.iov_len);
        if (rc < 0) {
            if (nwritten == 0)
                return rc;
            return nwritten;
        }
        nwritten += rc;
    }

    return nwritten;
}

ssize_t Process::do_write(FileDescription& description, const UserOrKernelBuffer& data, size_t data_size)
{
    ssize_t total_nwritten = 0;
    if (!description.is_blocking()) {
        if (!description.can_write())
            return -EAGAIN;
    }

    if (description.should_append())
        description.seek(0, SEEK_END);

    while ((size_t)total_nwritten < data_size) {
        if (!description.can_write()) {
            if (!description.is_blocking()) {
                // Short write: We can no longer write to this non-blocking description.
                ASSERT(total_nwritten > 0);
                return total_nwritten;
            }
            if (Thread::current()->block<Thread::WriteBlocker>(nullptr, description).was_interrupted()) {
                if (total_nwritten == 0)
                    return -EINTR;
            }
        }
        auto nwritten_or_error = description.write(data.offset(total_nwritten), data_size - total_nwritten);
        if (nwritten_or_error.is_error()) {
            if (total_nwritten)
                return total_nwritten;
            return nwritten_or_error.error();
        }
        if (nwritten_or_error.value() == 0)
            break;
        total_nwritten += nwritten_or_error.value();
    }
    return total_nwritten;
}

ssize_t Process::sys$write(int fd, const u8* data, ssize_t size)
{
    REQUIRE_PROMISE(stdio);
    if (size < 0)
        return -EINVAL;
    if (size == 0)
        return 0;

#ifdef DEBUG_IO
    dbg() << "sys$write(" << fd << ", " << (const void*)(data) << ", " << size << ")";
#endif
    auto description = file_description(fd);
    if (!description)
        return -EBADF;
    if (!description->is_writable())
        return -EBADF;

    auto buffer = UserOrKernelBuffer::for_user_buffer(const_cast<u8*>(data), (size_t)size);
    if (!buffer.has_value())
        return -EFAULT;
    return do_write(*description, buffer.value(), size);
}

}
