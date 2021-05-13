/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Userspace.h>
#include <Kernel/StdLib.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibC/errno_numbers.h>

namespace Kernel {

class [[nodiscard]] UserOrKernelBuffer {
public:
    UserOrKernelBuffer() = delete;

    static UserOrKernelBuffer for_kernel_buffer(u8* kernel_buffer)
    {
        VERIFY(!kernel_buffer || !is_user_address(VirtualAddress(kernel_buffer)));
        return UserOrKernelBuffer(kernel_buffer);
    }

    static Optional<UserOrKernelBuffer> for_user_buffer(u8* user_buffer, size_t size)
    {
        if (user_buffer && !is_user_range(VirtualAddress(user_buffer), size))
            return {};
        return UserOrKernelBuffer(user_buffer);
    }

    template<typename UserspaceType>
    static Optional<UserOrKernelBuffer> for_user_buffer(UserspaceType userspace, size_t size)
    {
        if (!is_user_range(VirtualAddress(userspace.unsafe_userspace_ptr()), size))
            return {};
        return UserOrKernelBuffer(const_cast<u8*>((const u8*)userspace.unsafe_userspace_ptr()));
    }

    [[nodiscard]] bool is_kernel_buffer() const;
    [[nodiscard]] const void* user_or_kernel_ptr() const { return m_buffer; }

    [[nodiscard]] UserOrKernelBuffer offset(ssize_t offset) const
    {
        if (!m_buffer)
            return *this;
        UserOrKernelBuffer offset_buffer = *this;
        offset_buffer.m_buffer += offset;
        VERIFY(offset_buffer.is_kernel_buffer() == is_kernel_buffer());
        return offset_buffer;
    }

    [[nodiscard]] String copy_into_string(size_t size) const;
    [[nodiscard]] bool write(const void* src, size_t offset, size_t len);
    [[nodiscard]] bool write(const void* src, size_t len)
    {
        return write(src, 0, len);
    }
    [[nodiscard]] bool write(ReadonlyBytes bytes)
    {
        return write(bytes.data(), bytes.size());
    }

    [[nodiscard]] bool read(void* dest, size_t offset, size_t len) const;
    [[nodiscard]] bool read(void* dest, size_t len) const
    {
        return read(dest, 0, len);
    }
    [[nodiscard]] bool read(Bytes bytes) const
    {
        return read(bytes.data(), bytes.size());
    }

    [[nodiscard]] bool memset(int value, size_t offset, size_t len);
    [[nodiscard]] bool memset(int value, size_t len)
    {
        return memset(value, 0, len);
    }

    template<size_t BUFFER_BYTES, typename F>
    [[nodiscard]] KResultOr<size_t> write_buffered(size_t offset, size_t len, F f)
    {
        if (!m_buffer)
            return EFAULT;
        if (is_kernel_buffer()) {
            // We're transferring directly to a kernel buffer, bypass
            return f(m_buffer + offset, len);
        }

        // The purpose of using a buffer on the stack is that we can
        // avoid a bunch of small (e.g. 1-byte) copy_to_user calls
        u8 buffer[BUFFER_BYTES];
        size_t nwritten = 0;
        while (nwritten < len) {
            auto to_copy = min(sizeof(buffer), len - nwritten);
            ssize_t copied = f(buffer, to_copy);
            if (copied < 0)
                return copied;
            VERIFY((size_t)copied <= to_copy);
            if (!write(buffer, nwritten, (size_t)copied))
                return EFAULT;
            nwritten += (size_t)copied;
            if ((size_t)copied < to_copy)
                break;
        }
        return nwritten;
    }
    template<size_t BUFFER_BYTES, typename F>
    [[nodiscard]] KResultOr<size_t> write_buffered(size_t len, F f)
    {
        return write_buffered<BUFFER_BYTES, F>(0, len, f);
    }

    template<size_t BUFFER_BYTES, typename F>
    [[nodiscard]] KResultOr<size_t> read_buffered(size_t offset, size_t len, F f) const
    {
        if (!m_buffer)
            return EFAULT;
        if (is_kernel_buffer()) {
            // We're transferring directly from a kernel buffer, bypass
            return f(m_buffer + offset, len);
        }

        // The purpose of using a buffer on the stack is that we can
        // avoid a bunch of small (e.g. 1-byte) copy_from_user calls
        u8 buffer[BUFFER_BYTES];
        size_t nread = 0;
        while (nread < len) {
            auto to_copy = min(sizeof(buffer), len - nread);
            if (!read(buffer, nread, to_copy))
                return EFAULT;
            ssize_t copied = f(buffer, to_copy);
            if (copied < 0)
                return copied;
            VERIFY((size_t)copied <= to_copy);
            nread += (size_t)copied;
            if ((size_t)copied < to_copy)
                break;
        }
        return nread;
    }
    template<size_t BUFFER_BYTES, typename F>
    [[nodiscard]] KResultOr<size_t> read_buffered(size_t len, F f) const
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
