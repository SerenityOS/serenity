#include "TTY.h"
#include "Process.h"
#include <LibC/errno_numbers.h>
#include <LibC/signal_numbers.h>
#include <LibC/sys/ioctl_numbers.h>

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
    ssize_t nread = min(m_buffer.size(), size);
    memcpy(buffer, m_buffer.data(), nread);
    if (nread == (ssize_t)m_buffer.size())
        m_buffer.clear();
    else {
        kprintf("had %u, read %u\n", m_buffer.size(), nread);
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
    dbgprintf("%s set_termios: IECHO? %u, ISIG? %u\n",
        ttyName().characters(),
        should_echo_input(),
        should_generate_signals());
}

int TTY::ioctl(Process& process, unsigned request, unsigned arg)
{
    if (process.tty() != this)
        return -ENOTTY;
    switch (request) {
    case TIOCGPGRP:
        return pgid();
    case TIOCSPGRP: {
        // FIXME: Validate pgid fully.
        pid_t pgid = static_cast<pid_t>(arg);
        if (pgid < 0)
            return -EINVAL;
        set_pgid(arg);
        return 0;
    }
    }
    return -EINVAL;
}
