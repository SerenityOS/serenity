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

inline void DoubleBuffer::compute_emptiness()
{
    m_empty = m_read_buffer_index >= m_read_buffer->size() && m_write_buffer->is_empty();
}

void DoubleBuffer::flip()
{
    ASSERT(m_read_buffer_index == m_read_buffer->size());
    swap(m_read_buffer, m_write_buffer);
    if (m_write_buffer->capacity() < 32)
        m_write_buffer->clear_with_capacity();
    else
        m_write_buffer->clear();
    m_read_buffer_index = 0;
    compute_emptiness();
}

ssize_t DoubleBuffer::write(const u8* data, ssize_t size)
{
    if (!size)
        return 0;
    LOCKER(m_lock);
    m_write_buffer->append(data, size);
    compute_emptiness();
    return size;
}

ssize_t DoubleBuffer::read(u8* data, ssize_t size)
{
    if (!size)
        return 0;
    LOCKER(m_lock);
    if (m_read_buffer_index >= m_read_buffer->size() && !m_write_buffer->is_empty())
        flip();
    if (m_read_buffer_index >= m_read_buffer->size())
        return 0;
    ssize_t nread = min((ssize_t)m_read_buffer->size() - m_read_buffer_index, size);
    memcpy(data, m_read_buffer->data() + m_read_buffer_index, nread);
    m_read_buffer_index += nread;
    compute_emptiness();
    return nread;
}
