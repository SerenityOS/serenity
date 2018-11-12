#include "FIFO.h"
#include <AK/StdLib.h>

//#define FIFO_DEBUG

RetainPtr<FIFO> FIFO::create()
{
    return adopt(*new FIFO);
}

FIFO::FIFO()
{
}

void FIFO::open(Direction direction)
{
    if (direction == Reader) {
        ++m_readers;
#ifdef FIFO_DEBUG
        kprintf("open reader (%u)\n", m_readers);
#endif
    } else if (direction == Writer) {
        ++m_writers;
#ifdef FIFO_DEBUG
        kprintf("open writer (%u)\n", m_writers);
#endif
    }
}

void FIFO::close(Direction direction)
{
    if (direction == Reader) {
#ifdef FIFO_DEBUG
        kprintf("close reader (%u - 1)\n", m_readers);
#endif
        ASSERT(m_readers);
        --m_readers;
    } else if (direction == Writer) {
#ifdef FIFO_DEBUG
        kprintf("close writer (%u - 1)\n", m_writers);
#endif
        ASSERT(m_writers);
        --m_writers;
    }
}

bool FIFO::can_read() const
{
    return !m_queue.isEmpty() || !m_writers;
}

bool FIFO::can_write() const
{
#ifdef FIFO_DEBUG
    dbgprintf("can_write? size(%u) < capacity(%u) || !readers(%u)\n", m_queue.size(), m_queue.capacity(), m_readers);
#endif
    return m_queue.size() < m_queue.capacity() || !m_readers;
}

ssize_t FIFO::read(byte* buffer, size_t size)
{
    if (!m_writers && m_queue.isEmpty())
        return 0;
#ifdef FIFO_DEBUG
    dbgprintf("fifo: read(%u)\n",size);
#endif
    size_t nread = min(size, m_queue.size());
    for (size_t i = 0; i < nread; ++i)
        buffer[i] = m_queue.dequeue();
#ifdef FIFO_DEBUG
    dbgprintf("   -> read (%c) %u\n", buffer[0], nread);
#endif
    return nread;
}

ssize_t FIFO::write(const byte* buffer, size_t size)
{
    if (!m_readers)
        return 0;
#ifdef FIFO_DEBUG
    dbgprintf("fifo: write(%p, %u)\n", buffer, size);
#endif
    size_t nwritten = min(size, m_queue.capacity() - m_queue.size());
    for (size_t i = 0; i < nwritten; ++i)
        m_queue.enqueue(buffer[i]);
    return nwritten;
}
