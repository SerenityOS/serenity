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
    for (auto& vec : vecs) {
        total_length += vec.iov_len;
        if (total_length > NumericLimits<i32>::max())
            return EINVAL;
    }

    auto description = file_description(fd);
    if (!description)
        return EBADF;

    if (!description->is_writable())
        return EBADF;

    int nwritten = 0;
    for (auto& vec : vecs) {
        auto buffer = UserOrKernelBuffer::for_user_buffer((u8*)vec.iov_base, vec.iov_len);
        if (!buffer.has_value())
            return EFAULT;
        auto result = do_write(*description, buffer.value(), vec.iov_len);
        if (result.is_error()) {
            if (nwritten == 0)
                return result.error();
            return nwritten;
        }
        nwritten += result.value();
    }

    return nwritten;
}

KResultOr<ssize_t> Process::do_write(FileDescription& description, const UserOrKernelBuffer& data, size_t data_size)
{
    ssize_t total_nwritten = 0;

    if (description.should_append() && description.file().is_seekable()) {
        auto seek_result = description.seek(0, SEEK_END);
        if (seek_result.is_error())
            return seek_result.error();
    }

    while ((size_t)total_nwritten < data_size) {
        while (!description.can_write()) {
            if (!description.is_blocking()) {
                if (total_nwritten > 0)
                    return total_nwritten;
                else
                    return EAGAIN;
            }
            auto unblock_flags = Thread::FileBlocker::BlockFlags::None;
            if (Thread::current()->block<Thread::WriteBlocker>({}, description, unblock_flags).was_interrupted()) {
                if (total_nwritten == 0)
                    return EINTR;
            }
            // TODO: handle exceptions in unblock_flags
        }
        auto nwritten_or_error = description.write(data.offset(total_nwritten), data_size - total_nwritten);
        if (nwritten_or_error.is_error()) {
            if (total_nwritten > 0)
                return total_nwritten;
            if (nwritten_or_error.error() == EAGAIN)
                continue;
            return nwritten_or_error.error();
        }
        VERIFY(nwritten_or_error.value() > 0);
        total_nwritten += nwritten_or_error.value();
    }
    return total_nwritten;
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

    auto buffer = UserOrKernelBuffer::for_user_buffer(data, static_cast<size_t>(size));
    if (!buffer.has_value())
        return EFAULT;
    return do_write(*description, buffer.value(), size);
}

}
