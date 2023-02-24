/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <AK/Userspace.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/Library/StdLib.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class [[nodiscard]] UserOrKernelBuffer {
public:
    UserOrKernelBuffer() = delete;

    static UserOrKernelBuffer for_kernel_buffer(u8* kernel_buffer)
    {
        VERIFY(!kernel_buffer || !Memory::is_user_address(VirtualAddress(kernel_buffer)));
        return UserOrKernelBuffer(kernel_buffer);
    }

    static ErrorOr<UserOrKernelBuffer> for_user_buffer(u8* user_buffer, size_t size)
    {
        if (user_buffer && !Memory::is_user_range(VirtualAddress(user_buffer), size))
            return Error::from_errno(EFAULT);
        return UserOrKernelBuffer(user_buffer);
    }

    template<typename UserspaceType>
    static ErrorOr<UserOrKernelBuffer> for_user_buffer(UserspaceType userspace, size_t size)
    {
        if (!Memory::is_user_range(VirtualAddress(userspace.unsafe_userspace_ptr()), size))
            return Error::from_errno(EFAULT);
        return UserOrKernelBuffer(const_cast<u8*>((u8 const*)userspace.unsafe_userspace_ptr()));
    }

    [[nodiscard]] bool is_kernel_buffer() const;
    [[nodiscard]] void const* user_or_kernel_ptr() const { return m_buffer; }

    [[nodiscard]] UserOrKernelBuffer offset(size_t offset) const
    {
        if (!m_buffer)
            return *this;
        UserOrKernelBuffer offset_buffer = *this;
        offset_buffer.m_buffer += offset;
        VERIFY(offset_buffer.is_kernel_buffer() == is_kernel_buffer());
        return offset_buffer;
    }

    ErrorOr<NonnullOwnPtr<KString>> try_copy_into_kstring(size_t) const;
    ErrorOr<void> write(void const* src, size_t offset, size_t len);
    ErrorOr<void> write(void const* src, size_t len)
    {
        return write(src, 0, len);
    }
    ErrorOr<void> write(ReadonlyBytes bytes)
    {
        return write(bytes.data(), bytes.size());
    }

    ErrorOr<void> read(void* dest, size_t offset, size_t len) const;
    ErrorOr<void> read(void* dest, size_t len) const
    {
        return read(dest, 0, len);
    }

    ErrorOr<void> read(Bytes bytes) const
    {
        return read(bytes.data(), bytes.size());
    }

    ErrorOr<void> memset(int value, size_t offset, size_t len);
    ErrorOr<void> memset(int value, size_t len)
    {
        return memset(value, 0, len);
    }

    template<size_t BUFFER_BYTES, typename F>
    ErrorOr<size_t> write_buffered(size_t offset, size_t len, F f)
    {
        if (!m_buffer)
            return EFAULT;
        if (is_kernel_buffer()) {
            // We're transferring directly to a kernel buffer, bypass
            Bytes bytes { m_buffer + offset, len };
            return f(bytes);
        }

        // The purpose of using a buffer on the stack is that we can
        // avoid a bunch of small (e.g. 1-byte) copy_to_user calls
        u8 buffer[BUFFER_BYTES];
        size_t nwritten = 0;
        while (nwritten < len) {
            auto to_copy = min(sizeof(buffer), len - nwritten);
            Bytes bytes { buffer, to_copy };
            ErrorOr<size_t> copied_or_error = f(bytes);
            if (copied_or_error.is_error())
                return copied_or_error.release_error();
            auto copied = copied_or_error.release_value();
            VERIFY(copied <= to_copy);
            TRY(write(buffer, nwritten, copied));
            nwritten += copied;
            if (copied < to_copy)
                break;
        }
        return nwritten;
    }
    template<size_t BUFFER_BYTES, typename F>
    ErrorOr<size_t> write_buffered(size_t len, F f)
    {
        return write_buffered<BUFFER_BYTES, F>(0, len, f);
    }

    template<size_t BUFFER_BYTES, typename F>
    ErrorOr<size_t> read_buffered(size_t offset, size_t len, F f) const
    {
        if (!m_buffer)
            return EFAULT;
        if (is_kernel_buffer()) {
            // We're transferring directly from a kernel buffer, bypass
            return f({ m_buffer + offset, len });
        }

        // The purpose of using a buffer on the stack is that we can
        // avoid a bunch of small (e.g. 1-byte) copy_from_user calls
        u8 buffer[BUFFER_BYTES];
        size_t nread = 0;
        while (nread < len) {
            auto to_copy = min(sizeof(buffer), len - nread);
            TRY(read(buffer, nread, to_copy));
            ReadonlyBytes read_only_bytes { buffer, to_copy };
            ErrorOr<size_t> copied_or_error = f(read_only_bytes);
            if (copied_or_error.is_error())
                return copied_or_error.release_error();
            auto copied = copied_or_error.release_value();
            VERIFY(copied <= to_copy);
            nread += copied;
            if (copied < to_copy)
                break;
        }
        return nread;
    }
    template<size_t BUFFER_BYTES, typename F>
    ErrorOr<size_t> read_buffered(size_t len, F f) const
    {
        return read_buffered<BUFFER_BYTES, F>(0, len, f);
    }

private:
    explicit UserOrKernelBuffer(u8* buffer)
        : m_buffer(buffer)
    {
    }

    u8* m_buffer;
};

}
