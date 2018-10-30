#include "TTY.h"

TTY::TTY(unsigned major, unsigned minor)
    : CharacterDevice(major, minor)
{
}

TTY::~TTY()
{
}

ssize_t TTY::read(byte* buffer, size_t size)
{
    ssize_t nread = min(m_buffer.size(), size);
    memcpy(buffer, m_buffer.data(), nread);
    if (nread == m_buffer.size())
        m_buffer.clear();
    else {
        dbgprintf("had %u, read %u\n", m_buffer.size(), nread);
        ASSERT_NOT_REACHED();
    }
    return nread;
}

ssize_t TTY::write(const byte* buffer, size_t size)
{
    for (size_t i = 0; i < size; ++i)
        onTTYWrite(buffer[i]);
    return 0;
}

bool TTY::hasDataAvailableForRead() const
{
    return !m_buffer.isEmpty();
}

void TTY::emit(byte ch)
{
    m_buffer.append(ch);
}
