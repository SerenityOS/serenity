#include "DoubleBuffer.h"

void DoubleBuffer::flip()
{
    InterruptDisabler disabler;
    ASSERT(m_read_buffer_index == m_read_buffer->size());
    swap(m_read_buffer, m_write_buffer);
    if (m_write_buffer->capacity() < 32)
        m_write_buffer->clear_with_capacity();
    else
        m_write_buffer->clear();
    m_read_buffer_index = 0;
}

ssize_t DoubleBuffer::write(const byte* data, size_t size)
{
    m_write_buffer->append(data, size);
    return size;
}

ssize_t DoubleBuffer::read(byte* data, size_t size)
{
    if (m_read_buffer_index >= m_read_buffer->size() && !m_write_buffer->is_empty())
        flip();
    if (m_read_buffer_index >= m_read_buffer->size())
        return 0;
    ssize_t nread = min(m_read_buffer->size() - m_read_buffer_index, size);
    memcpy(data, m_read_buffer->data() + m_read_buffer_index, nread);
    m_read_buffer_index += nread;
    return nread;
}
