/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumericLimits.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$writev(int fd, Userspace<const struct iovec*> iov, int iov_count)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);
    if (iov_count < 0)
        return EINVAL;

    // Arbitrary pain threshold.
    if (iov_count > (int)MiB)
        return EFAULT;

    u64 total_length = 0;
    Vector<iovec, 32> vecs;
    TRY(vecs.try_resize(iov_count));
    TRY(copy_n_from_user(vecs.data(), iov, iov_count));
    for (auto& vec : vecs) {
        total_length += vec.iov_len;
        if (total_length > NumericLimits<i32>::max())
            return EINVAL;
    }

    auto description = TRY(fds().open_file_description(fd));
    if (!description->is_writable())
        return EBADF;

    int nwritten = 0;
    for (auto& vec : vecs) {
        auto buffer = TRY(UserOrKernelBuffer::for_user_buffer((u8*)vec.iov_base, vec.iov_len));
        auto result = do_write(*description, buffer, vec.iov_len);
        if (result.is_error()) {
            if (nwritten == 0)
                return result.release_error();
            return nwritten;
        }
        nwritten += result.value();
    }

    return nwritten;
}

ErrorOr<FlatPtr> Process::do_write(OpenFileDescription& description, const UserOrKernelBuffer& data, size_t data_size)
{
    size_t total_nwritten = 0;

    if (description.should_append() && description.file().is_seekable()) {
        TRY(description.seek(0, SEEK_END));
    }

    while (total_nwritten < data_size) {
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
            if (nwritten_or_error.error().code() == EAGAIN)
                continue;
            return nwritten_or_error.release_error();
        }
        VERIFY(nwritten_or_error.value() > 0);
        total_nwritten += nwritten_or_error.value();
    }
    return total_nwritten;
}

ErrorOr<FlatPtr> Process::sys$write(int fd, Userspace<const u8*> data, size_t size)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(stdio);
    if (size == 0)
        return 0;
    if (size > NumericLimits<ssize_t>::max())
        return EINVAL;

    dbgln_if(IO_DEBUG, "sys$write({}, {}, {})", fd, data.ptr(), size);
    auto description = TRY(fds().open_file_description(fd));
    if (!description->is_writable())
        return EBADF;

    auto buffer = TRY(UserOrKernelBuffer::for_user_buffer(data, static_cast<size_t>(size)));
    return do_write(*description, buffer, size);
}

}
