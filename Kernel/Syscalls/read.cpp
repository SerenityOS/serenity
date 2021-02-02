/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

using BlockFlags = Thread::FileBlocker::BlockFlags;

KResultOr<ssize_t> Process::sys$readv(int fd, Userspace<const struct iovec*> iov, int iov_count)
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

    if (!description->is_readable())
        return EBADF;

    if (description->is_directory())
        return EISDIR;

    auto unblock_flags = BlockFlags::None;
    KResultOr<size_t> read_result(KSuccess);
    if (Thread::current()->block<Thread::ReadBlocker>({}, *description, unblock_flags, buffers.data(), buffers.size(), read_result).was_interrupted())
        return EINTR;
    if (read_result.is_error())
        return read_result.error();
    if (!has_flag(unblock_flags, BlockFlags::Read))
        return EAGAIN;
    // TODO: handle exceptions in unblock_flags
    return read_result.value();
}

KResultOr<ssize_t> Process::sys$read(int fd, Userspace<u8*> buffer, ssize_t size)
{
    REQUIRE_PROMISE(stdio);
    if (size < 0)
        return EINVAL;
    if (size == 0)
        return 0;
    dbgln_if(IO_DEBUG, "sys$read({}, {}, {})", fd, buffer.ptr(), size);
    auto description = file_description(fd);
    if (!description)
        return EBADF;
    if (!description->is_readable())
        return EBADF;
    if (description->is_directory())
        return EISDIR;

    auto user_buffer = UserOrKernelBuffer::for_user_buffer(buffer, size);
    if (!user_buffer.has_value())
        return EFAULT;
    UserOrKernelBufferWithSize buffer_with_size { user_buffer.release_value(), (size_t)size };

    auto unblock_flags = BlockFlags::None;
    KResultOr<size_t> read_result(KSuccess);
    if (Thread::current()->block<Thread::ReadBlocker>({}, *description, unblock_flags, &buffer_with_size, 1, read_result).was_interrupted())
        return EINTR;
    if (read_result.is_error())
        return read_result.error();
    if (!has_flag(unblock_flags, BlockFlags::Read))
        return EAGAIN;
    // TODO: handle exceptions in unblock_flags
    return read_result.value();
}

}
