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

#include <Kernel/Debug.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

ssize_t Process::sys$readv(int fd, Userspace<const struct iovec*> iov, int iov_count)
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

    if (!description->is_readable())
        return -EBADF;

    if (description->is_directory())
        return -EISDIR;

    int nread = 0;
    for (auto& vec : vecs) {
        if (description->is_blocking()) {
            if (!description->can_read()) {
                auto unblock_flags = Thread::FileBlocker::BlockFlags::None;
                if (Thread::current()->block<Thread::ReadBlocker>({}, *description, unblock_flags).was_interrupted())
                    return -EINTR;
                if (!((u32)unblock_flags & (u32)Thread::FileBlocker::BlockFlags::Read))
                    return -EAGAIN;
                // TODO: handle exceptions in unblock_flags
            }
        }
        auto buffer = UserOrKernelBuffer::for_user_buffer((u8*)vec.iov_base, vec.iov_len);
        if (!buffer.has_value())
            return -EFAULT;
        auto result = description->read(buffer.value(), vec.iov_len);
        if (result.is_error())
            return result.error();
        nread += result.value();
    }

    return nread;
}

ssize_t Process::sys$read(int fd, Userspace<u8*> buffer, ssize_t size)
{
    REQUIRE_PROMISE(stdio);
    if (size < 0)
        return -EINVAL;
    if (size == 0)
        return 0;
    dbgln_if(IO_DEBUG, "sys$read({}, {}, {})", fd, buffer.ptr(), size);
    auto description = file_description(fd);
    if (!description)
        return -EBADF;
    if (!description->is_readable())
        return -EBADF;
    if (description->is_directory())
        return -EISDIR;
    if (description->is_blocking()) {
        if (!description->can_read()) {
            auto unblock_flags = Thread::FileBlocker::BlockFlags::None;
            if (Thread::current()->block<Thread::ReadBlocker>({}, *description, unblock_flags).was_interrupted())
                return -EINTR;
            if (!((u32)unblock_flags & (u32)Thread::FileBlocker::BlockFlags::Read))
                return -EAGAIN;
            // TODO: handle exceptions in unblock_flags
        }
    }
    auto user_buffer = UserOrKernelBuffer::for_user_buffer(buffer, size);
    if (!user_buffer.has_value())
        return -EFAULT;
    auto result = description->read(user_buffer.value(), size);
    if (result.is_error())
        return result.error();
    return result.value();
}

}
