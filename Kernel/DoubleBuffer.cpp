/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/DoubleBuffer.h>

namespace Kernel {

inline void DoubleBuffer::compute_lockfree_metadata()
{
    InterruptDisabler disabler;
    m_empty = m_read_buffer_index >= m_read_buffer->size && m_write_buffer->size == 0;
    m_space_for_writing = m_capacity - m_write_buffer->size;
}

DoubleBuffer::DoubleBuffer(size_t capacity)
    : m_write_buffer(&m_buffer1)
    , m_read_buffer(&m_buffer2)
    , m_storage(KBuffer::create_with_size(capacity * 2, Region::Access::Read | Region::Access::Write, "DoubleBuffer"))
    , m_capacity(capacity)
{
    m_buffer1.data = m_storage.data();
    m_buffer1.size = 0;
    m_buffer2.data = m_storage.data() + capacity;
    m_buffer2.size = 0;
    m_space_for_writing = capacity;
}

void DoubleBuffer::flip()
{
    if (m_storage.is_null())
        return;
    VERIFY(m_read_buffer_index == m_read_buffer->size);
    swap(m_read_buffer, m_write_buffer);
    m_write_buffer->size = 0;
    m_read_buffer_index = 0;
    compute_lockfree_metadata();
}

ssize_t DoubleBuffer::write(const UserOrKernelBuffer& data, size_t size)
{
    if (!size || m_storage.is_null())
        return 0;
    Locker locker(m_lock);
    size_t bytes_to_write = min(size, m_space_for_writing);
    u8* write_ptr = m_write_buffer->data + m_write_buffer->size;
    if (!data.read(write_ptr, bytes_to_write))
        return -EFAULT;
    m_write_buffer->size += bytes_to_write;
    compute_lockfree_metadata();
    if (m_unblock_callback && !m_empty)
        m_unblock_callback();
    return (ssize_t)bytes_to_write;
}

ssize_t DoubleBuffer::read(UserOrKernelBuffer& data, size_t size)
{
    if (!size || m_storage.is_null())
        return 0;
    Locker locker(m_lock);
    if (m_read_buffer_index >= m_read_buffer->size && m_write_buffer->size != 0)
        flip();
    if (m_read_buffer_index >= m_read_buffer->size)
        return 0;
    size_t nread = min(m_read_buffer->size - m_read_buffer_index, size);
    if (!data.write(m_read_buffer->data + m_read_buffer_index, nread))
        return -EFAULT;
    m_read_buffer_index += nread;
    compute_lockfree_metadata();
    if (m_unblock_callback && m_space_for_writing > 0)
        m_unblock_callback();
    return (ssize_t)nread;
}

ssize_t DoubleBuffer::peek(UserOrKernelBuffer& data, size_t size)
{
    if (!size || m_storage.is_null())
        return 0;
    Locker locker(m_lock);
    if (m_read_buffer_index >= m_read_buffer->size && m_write_buffer->size != 0) {
        flip();
    }
    if (m_read_buffer_index >= m_read_buffer->size)
        return 0;
    size_t nread = min(m_read_buffer->size - m_read_buffer_index, size);
    if (!data.write(m_read_buffer->data + m_read_buffer_index, nread))
        return -EFAULT;
    compute_lockfree_metadata();
    if (m_unblock_callback && m_space_for_writing > 0)
        m_unblock_callback();
    return (ssize_t)nread;
}

}
