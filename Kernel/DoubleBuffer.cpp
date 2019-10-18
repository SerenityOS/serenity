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
    , m_storage(KBuffer::create_with_size(capacity * 2))
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
