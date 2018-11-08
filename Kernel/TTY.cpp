#include "TTY.h"
#include "Process.h"
#include <LibC/signal_numbers.h>

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
    onTTYWrite(buffer, size);
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

void TTY::interrupt()
{
    dbgprintf("%s: Interrupt ^C pressed!\n", ttyName().characters());
    if (pgid()) {
        dbgprintf("%s: Send SIGINT to everyone in pgrp %d\n", ttyName().characters(), pgid());
        InterruptDisabler disabler;
        Process::for_each_in_pgrp(pgid(), [this] (auto& process) {
            dbgprintf("%s: Send SIGINT to %d\n", ttyName().characters(), process.pid());
            process.send_signal(SIGINT, nullptr);
            return true;
        });
    }
}
