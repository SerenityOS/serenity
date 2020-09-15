/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#pragma once

#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Userspace.h>
#include <Kernel/StdLib.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibC/errno_numbers.h>

namespace Kernel {

class UserOrKernelBuffer {
public:
    UserOrKernelBuffer() = delete;

    static UserOrKernelBuffer for_kernel_buffer(u8* kernel_buffer)
    {
        ASSERT(!kernel_buffer || !is_user_address(VirtualAddress(kernel_buffer)));
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

    bool is_kernel_buffer() const;
    const void* user_or_kernel_ptr() const { return m_buffer; }

    UserOrKernelBuffer offset(ssize_t offset) const
    {
        if (!m_buffer)
            return *this;
        UserOrKernelBuffer offset_buffer = *this;
        offset_buffer.m_buffer += offset;
        ASSERT(offset_buffer.is_kernel_buffer() == is_kernel_buffer());
        return offset_buffer;
    }

    String copy_into_string(size_t size) const;
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
    [[nodiscard]] ssize_t write_buffered(size_t offset, size_t len, F f)
    {
        if (!m_buffer)
            return -EFAULT;
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
            ASSERT((size_t)copied <= to_copy);
            if (!write(buffer, nwritten, (size_t)copied))
                return -EFAULT;
            nwritten += (size_t)copied;
            if ((size_t)copied < to_copy)
                break;
        }
        return (ssize_t)nwritten;
    }
    template<size_t BUFFER_BYTES, typename F>
    [[nodiscard]] ssize_t write_buffered(size_t len, F f)
    {
        return write_buffered<BUFFER_BYTES, F>(0, len, f);
    }

    template<size_t BUFFER_BYTES, typename F>
    [[nodiscard]] ssize_t read_buffered(size_t offset, size_t len, F f) const
    {
        if (!m_buffer)
            return -EFAULT;
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
                return -EFAULT;
            ssize_t copied = f(buffer, to_copy);
            if (copied < 0)
                return copied;
            ASSERT((size_t)copied <= to_copy);
            nread += (size_t)copied;
            if ((size_t)copied < to_copy)
                break;
        }
        return nread;
    }
    template<size_t BUFFER_BYTES, typename F>
    [[nodiscard]] ssize_t read_buffered(size_t len, F f) const
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
