#include "TTY.h"
#include "Process.h"
#include <LibC/errno_numbers.h>
#include <LibC/signal_numbers.h>
#include <LibC/sys/ioctl_numbers.h>

//#define TTY_DEBUG

void DoubleBuffer::flip()
{
    ASSERT(m_read_buffer_index == m_read_buffer->size());
    swap(m_read_buffer, m_write_buffer);
    m_write_buffer->clear();
    m_read_buffer_index = 0;
}

Unix::ssize_t DoubleBuffer::write(const byte* data, size_t size)
{
    m_write_buffer->append(data, size);
    return size;
}

Unix::ssize_t DoubleBuffer::read(byte* data, size_t size)
{
    if (m_read_buffer_index >= m_read_buffer->size() && !m_write_buffer->isEmpty())
        flip();
    if (m_read_buffer_index >= m_read_buffer->size())
        return 0;
    ssize_t nread = min(m_read_buffer->size() - m_read_buffer_index, size);
    memcpy(data, m_read_buffer->data() + m_read_buffer_index, nread);
    m_read_buffer_index += nread;
    return nread;
}

TTY::TTY(unsigned major, unsigned minor)
    : CharacterDevice(major, minor)
{
    memset(&m_termios, 0, sizeof(m_termios));
    m_termios.c_lflag |= ISIG | ECHO;
}

TTY::~TTY()
{
}

ssize_t TTY::read(byte* buffer, size_t size)
{
    return m_buffer.read(buffer, size);
}

ssize_t TTY::write(const byte* buffer, size_t size)
{
#ifdef TTY_DEBUG
    dbgprintf("TTY::write %b    {%u}\n", buffer[0], size);
#endif
    onTTYWrite(buffer, size);
    return 0;
}

bool TTY::hasDataAvailableForRead() const
{
    return !m_buffer.is_empty();
}

void TTY::emit(byte ch)
{
    m_buffer.write(&ch, 1);
}

void TTY::interrupt()
{
    if (!should_generate_signals())
        return;
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

void TTY::set_termios(const Unix::termios& t)
{
    m_termios = t;
    dbgprintf("%s set_termios: IECHO? %u, ISIG? %u, ICANON? %u\n",
        ttyName().characters(),
        should_echo_input(),
        should_generate_signals(),
        in_canonical_mode()
    );
}

int TTY::ioctl(Process& process, unsigned request, unsigned arg)
{
    pid_t pgid;
    Unix::termios* tp;

    if (process.tty() != this)
        return -ENOTTY;
    switch (request) {
    case TIOCGPGRP:
        return m_pgid;
    case TIOCSPGRP:
        // FIXME: Validate pgid fully.
        pgid = static_cast<pid_t>(arg);
        if (pgid < 0)
            return -EINVAL;
        m_pgid = pgid;
        return 0;
    case TCGETS:
        tp = reinterpret_cast<Unix::termios*>(arg);
        if (!process.validate_write(tp, sizeof(Unix::termios)))
            return -EFAULT;
        *tp = m_termios;
        return 0;
    case TCSETS:
    case TCSETSF:
    case TCSETSW:
        tp = reinterpret_cast<Unix::termios*>(arg);
        if (!process.validate_read(tp, sizeof(Unix::termios)))
            return -EFAULT;
        set_termios(*tp);
        return 0;
    }
    ASSERT_NOT_REACHED();
    return -EINVAL;
}
