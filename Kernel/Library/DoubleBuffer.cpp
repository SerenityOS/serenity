/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Library/DoubleBuffer.h>

namespace Kernel {

inline void DoubleBuffer::compute_lockfree_metadata()
{
    InterruptDisabler disabler;
    m_empty = m_read_buffer_index >= m_read_buffer->size && m_write_buffer->size == 0;
    m_space_for_writing = m_capacity - m_write_buffer->size;
}

ErrorOr<NonnullOwnPtr<DoubleBuffer>> DoubleBuffer::try_create(StringView name, size_t capacity)
{
    auto storage = TRY(KBuffer::try_create_with_size(name, capacity * 2, Memory::Region::Access::ReadWrite));
    return adopt_nonnull_own_or_enomem(new (nothrow) DoubleBuffer(capacity, move(storage)));
}

DoubleBuffer::DoubleBuffer(size_t capacity, NonnullOwnPtr<KBuffer> storage)
    : m_write_buffer(&m_buffer1)
    , m_read_buffer(&m_buffer2)
    , m_storage(move(storage))
    , m_capacity(capacity)
{
    m_buffer1.data = m_storage->data();
    m_buffer1.size = 0;
    m_buffer2.data = m_storage->data() + capacity;
    m_buffer2.size = 0;
    m_space_for_writing = capacity;
}

void DoubleBuffer::flip()
{
    VERIFY(m_read_buffer_index == m_read_buffer->size);
    swap(m_read_buffer, m_write_buffer);
    m_write_buffer->size = 0;
    m_read_buffer_index = 0;
    compute_lockfree_metadata();
}

ErrorOr<size_t> DoubleBuffer::write(UserOrKernelBuffer const& data, size_t size)
{
    if (!size)
        return 0;
    MutexLocker locker(m_lock);
    size_t bytes_to_write = min(size, m_space_for_writing);
    u8* write_ptr = m_write_buffer->data + m_write_buffer->size;
    TRY(data.read(write_ptr, bytes_to_write));
    m_write_buffer->size += bytes_to_write;
    compute_lockfree_metadata();
    if (m_unblock_callback && !m_empty)
        m_unblock_callback();
    return bytes_to_write;
}

ErrorOr<size_t> DoubleBuffer::read_impl(UserOrKernelBuffer& data, size_t size, MutexLocker&, bool advance_buffer_index)
{
    if (size == 0)
        return 0;
    if (m_read_buffer_index >= m_read_buffer->size && m_write_buffer->size != 0)
        flip();
    if (m_read_buffer_index >= m_read_buffer->size)
        return 0;
    size_t nread = min(m_read_buffer->size - m_read_buffer_index, size);
    TRY(data.write(m_read_buffer->data + m_read_buffer_index, nread));
    if (advance_buffer_index)
        m_read_buffer_index += nread;
    compute_lockfree_metadata();
    if (m_unblock_callback && m_space_for_writing > 0)
        m_unblock_callback();
    return nread;
}

ErrorOr<size_t> DoubleBuffer::read(UserOrKernelBuffer& data, size_t size)
{
    MutexLocker locker(m_lock);
    return read_impl(data, size, locker, true);
}

ErrorOr<size_t> DoubleBuffer::peek(UserOrKernelBuffer& data, size_t size)
{
    MutexLocker locker(m_lock);
    return read_impl(data, size, locker, false);
}

}
