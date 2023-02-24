/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Library/KBuffer.h>
#include <Kernel/Library/UserOrKernelBuffer.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Tasks/Thread.h>

namespace Kernel {

class DoubleBuffer {
public:
    static ErrorOr<NonnullOwnPtr<DoubleBuffer>> try_create(StringView name, size_t capacity = 65536);
    ErrorOr<size_t> write(UserOrKernelBuffer const&, size_t);
    ErrorOr<size_t> write(u8 const* data, size_t size)
    {
        return write(UserOrKernelBuffer::for_kernel_buffer(const_cast<u8*>(data)), size);
    }
    ErrorOr<size_t> read(UserOrKernelBuffer&, size_t);
    ErrorOr<size_t> read(u8* data, size_t size)
    {
        auto buffer = UserOrKernelBuffer::for_kernel_buffer(data);
        return read(buffer, size);
    }
    ErrorOr<size_t> peek(UserOrKernelBuffer&, size_t);
    ErrorOr<size_t> peek(u8* data, size_t size)
    {
        auto buffer = UserOrKernelBuffer::for_kernel_buffer(data);
        return peek(buffer, size);
    }

    bool is_empty() const { return m_empty; }

    size_t space_for_writing() const { return m_space_for_writing; }
    size_t immediately_readable() const
    {
        return (m_read_buffer->size - m_read_buffer_index) + m_write_buffer->size;
    }

    void set_unblock_callback(Function<void()> callback)
    {
        VERIFY(!m_unblock_callback);
        m_unblock_callback = move(callback);
    }

private:
    explicit DoubleBuffer(size_t capacity, NonnullOwnPtr<KBuffer> storage);
    void flip();
    void compute_lockfree_metadata();

    ErrorOr<size_t> read_impl(UserOrKernelBuffer&, size_t, MutexLocker&, bool advance_buffer_index);

    struct InnerBuffer {
        u8* data { nullptr };
        size_t size { 0 };
    };

    InnerBuffer* m_write_buffer { nullptr };
    InnerBuffer* m_read_buffer { nullptr };
    InnerBuffer m_buffer1;
    InnerBuffer m_buffer2;

    NonnullOwnPtr<KBuffer> m_storage;
    Function<void()> m_unblock_callback;
    size_t m_capacity { 0 };
    size_t m_read_buffer_index { 0 };
    size_t m_space_for_writing { 0 };
    bool m_empty { true };
    mutable Mutex m_lock { "DoubleBuffer"sv };
};

}
