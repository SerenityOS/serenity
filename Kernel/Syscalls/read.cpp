/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/PerformanceManager.h>
#include <Kernel/Process.h>

namespace Kernel {

using BlockFlags = Thread::FileBlocker::BlockFlags;

static ErrorOr<NonnullRefPtr<OpenFileDescription>> open_readable_file_description(auto& fds, int fd)
{
    auto description = TRY(fds.with_shared([&](auto& fds) { return fds.open_file_description(fd); }));
    if (!description->is_readable())
        return EBADF;
    if (description->is_directory())
        return EISDIR;
    return description;
}

static ErrorOr<void> check_blocked_read(OpenFileDescription* description)
{
    if (description->is_blocking()) {
        if (!description->can_read()) {
            auto unblock_flags = BlockFlags::None;
            if (Thread::current()->block<Thread::ReadBlocker>({}, *description, unblock_flags).was_interrupted())
                return EINTR;
            if (!has_flag(unblock_flags, BlockFlags::Read))
                return EAGAIN;
            // TODO: handle exceptions in unblock_flags
        }
    }
    return {};
}

ErrorOr<FlatPtr> Process::sys$readv(int fd, Userspace<const struct iovec*> iov, int iov_count)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::stdio));
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

    auto description = TRY(open_readable_file_description(fds(), fd));

    int nread = 0;
    for (auto& vec : vecs) {
        TRY(check_blocked_read(description));
        auto buffer = TRY(UserOrKernelBuffer::for_user_buffer((u8*)vec.iov_base, vec.iov_len));
        auto nread_here = TRY(description->read(buffer, vec.iov_len));
        nread += nread_here;
    }

    return nread;
}

ErrorOr<FlatPtr> Process::sys$read(int fd, Userspace<u8*> buffer, size_t size)
{
    const auto start_timestamp = TimeManagement::the().uptime_ms();
    const auto result = read_impl(fd, buffer, size);

    if (Thread::current()->is_profiling_suppressed())
        return result;

    auto description = TRY(open_readable_file_description(fds(), fd));
    PerformanceManager::add_read_event(*Thread::current(), fd, size, description, start_timestamp, result);

    return result;
}

ErrorOr<FlatPtr> Process::read_impl(int fd, Userspace<u8*> buffer, size_t size)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::stdio));
    if (size == 0)
        return 0;
    if (size > NumericLimits<ssize_t>::max())
        return EINVAL;
    dbgln_if(IO_DEBUG, "sys$read({}, {}, {})", fd, buffer.ptr(), size);
    auto description = TRY(open_readable_file_description(fds(), fd));

    TRY(check_blocked_read(description));
    auto user_buffer = TRY(UserOrKernelBuffer::for_user_buffer(buffer, size));
    return TRY(description->read(user_buffer, size));
}

// NOTE: The offset is passed by pointer because off_t is 64bit,
// hence it can't be passed by register on 32bit platforms.
ErrorOr<FlatPtr> Process::sys$pread(int fd, Userspace<u8*> buffer, size_t size, Userspace<off_t const*> userspace_offset)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::stdio));
    if (size == 0)
        return 0;
    if (size > NumericLimits<ssize_t>::max())
        return EINVAL;
    auto offset = TRY(copy_typed_from_user(userspace_offset));
    if (offset < 0)
        return EINVAL;
    dbgln_if(IO_DEBUG, "sys$pread({}, {}, {}, {})", fd, buffer.ptr(), size, offset);
    auto description = TRY(open_readable_file_description(fds(), fd));
    if (!description->file().is_seekable())
        return EINVAL;
    TRY(check_blocked_read(description));
    auto user_buffer = TRY(UserOrKernelBuffer::for_user_buffer(buffer, size));
    return TRY(description->read(user_buffer, offset, size));
}

}
