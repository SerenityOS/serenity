/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumericLimits.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<ssize_t> Process::sys$writev(int fd, Userspace<const struct iovec*> iov, int iov_count)
{
    REQUIRE_PROMISE(stdio);
    if (iov_count < 0)
        return EINVAL;

    // Arbitrary pain threshold.
    if (iov_count > (int)MiB)
        return EFAULT;

    u64 total_length = 0;
    Vector<iovec, 32> vecs;
    if (!vecs.try_resize(iov_count))
        return ENOMEM;
    if (!copy_n_from_user(vecs.data(), iov, iov_count))
        return EFAULT;
    Vector<UserOrKernelBufferWithSize, 32> buffers;
    for (auto& vec : vecs) {
        total_length += vec.iov_len;
        if (total_length > NumericLimits<i32>::max())
            return EINVAL;
        auto buffer = UserOrKernelBuffer::for_user_buffer((u8*)vec.iov_base, vec.iov_len);
        if (!buffer.has_value())
            return EFAULT;
        buffers.append({ buffer.release_value(), vec.iov_len });
    }

    auto description = file_description(fd);
    if (!description)
        return EBADF;

    if (!description->is_writable())
        return EBADF;

    // TODO: Move into WriteBlocker
    if (description->should_append() && description->file().is_seekable()) {
        auto seek_result = description->seek(0, SEEK_END);
        if (seek_result.is_error())
            return seek_result.error();
    }

    auto unblock_flags = Thread::FileBlocker::BlockFlags::None;
    KResultOr<size_t> write_result(KSuccess);
    if (Thread::current()->block<Thread::WriteBlocker>({}, *description, unblock_flags, buffers.data(), buffers.size(), write_result).was_interrupted())
        return EINTR;
    if (write_result.is_error())
        return write_result.error();
    return (ssize_t)write_result.value();
}

KResultOr<ssize_t> Process::sys$write(int fd, Userspace<const u8*> data, ssize_t size)
{
    REQUIRE_PROMISE(stdio);
    if (size < 0)
        return EINVAL;
    if (size == 0)
        return 0;

    dbgln_if(IO_DEBUG, "sys$write({}, {}, {})", fd, data.ptr(), size);
    auto description = file_description(fd);
    if (!description)
        return EBADF;
    if (!description->is_writable())
        return EBADF;

    auto user_buffer = UserOrKernelBuffer::for_user_buffer(const_cast<u8*>(data.unsafe_userspace_ptr()), (size_t)size);
    if (!user_buffer.has_value())
        return EFAULT;
    UserOrKernelBufferWithSize buffer { user_buffer.release_value(), (size_t)size };
    // TODO: Move into WriteBlocker
    if (description->should_append()) {
        auto seek_result = description->seek(0, SEEK_END);
        if (seek_result.is_error())
            return seek_result.error();
    }

    auto unblock_flags = Thread::FileBlocker::BlockFlags::None;
    KResultOr<size_t> write_result(KSuccess);
    if (Thread::current()->block<Thread::WriteBlocker>({}, *description, unblock_flags, &buffer, 1, write_result).was_interrupted())
        return EINTR;
    if (write_result.is_error())
        return write_result.error();
    return (ssize_t)write_result.value();
}

}
