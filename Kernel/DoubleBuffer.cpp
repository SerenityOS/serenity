/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <Kernel/DoubleBuffer.h>

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
    ASSERT(m_read_buffer_index == m_read_buffer->size);
    swap(m_read_buffer, m_write_buffer);
    m_write_buffer->size = 0;
    m_read_buffer_index = 0;
    compute_lockfree_metadata();
}

ssize_t DoubleBuffer::write(const u8* data, ssize_t size)
{
    if (!size)
        return 0;
    LOCKER(m_lock);
    ASSERT(size <= (ssize_t)space_for_writing());
    u8* write_ptr = m_write_buffer->data + m_write_buffer->size;
    m_write_buffer->size += size;
    compute_lockfree_metadata();
    memcpy(write_ptr, data, size);
    return size;
}

ssize_t DoubleBuffer::read(u8* data, ssize_t size)
{
    if (!size)
        return 0;
    LOCKER(m_lock);
    if (m_read_buffer_index >= m_read_buffer->size && m_write_buffer->size != 0)
        flip();
    if (m_read_buffer_index >= m_read_buffer->size)
        return 0;
    ssize_t nread = min((ssize_t)m_read_buffer->size - (ssize_t)m_read_buffer_index, size);
    memcpy(data, m_read_buffer->data + m_read_buffer_index, nread);
    m_read_buffer_index += nread;
    compute_lockfree_metadata();
    return nread;
}
