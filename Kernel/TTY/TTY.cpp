#include <Kernel/TTY/TTY.h>
#include "Process.h"
#include <LibC/errno_numbers.h>
#include <LibC/signal_numbers.h>
#include <LibC/sys/ioctl_numbers.h>

//#define TTY_DEBUG

TTY::TTY(unsigned major, unsigned minor)
    : CharacterDevice(major, minor)
{
    set_default_termios();
}

TTY::~TTY()
{
}

void TTY::set_default_termios()
{
    memset(&m_termios, 0, sizeof(m_termios));
    m_termios.c_lflag |= ISIG | ECHO;
    static const char default_cc[32] = "\003\034\177\025\004\0\1\0\021\023\032\0\022\017\027\026\0";
    memcpy(m_termios.c_cc, default_cc, sizeof(default_cc));
}

ssize_t TTY::read(FileDescriptor&, byte* buffer, ssize_t size)
{
    return m_buffer.read(buffer, size);
}

ssize_t TTY::write(FileDescriptor&, const byte* buffer, ssize_t size)
{
#ifdef TTY_DEBUG
    dbgprintf("TTY::write {%u} ", size);
    for (size_t i = 0; i < size; ++i) {
        dbgprintf("%b ", buffer[i]);
    }
    dbgprintf("\n");
#endif
    on_tty_write(buffer, size);
    return size;
}

bool TTY::can_read(FileDescriptor&) const
{
    return !m_buffer.is_empty();
}

bool TTY::can_write(FileDescriptor&) const
{
    return true;
}

void TTY::emit(byte ch)
{
    if (should_generate_signals()) {
        if (ch == m_termios.c_cc[VINTR]) {
            dbgprintf("%s: VINTR pressed!\n", tty_name().characters());
            generate_signal(SIGINT);
            return;
        }
        if (ch == m_termios.c_cc[VQUIT]) {
            dbgprintf("%s: VQUIT pressed!\n", tty_name().characters());
            generate_signal(SIGQUIT);
            return;
        }
    }
    m_buffer.write(&ch, 1);
}

void TTY::generate_signal(int signal)
{
    if (!pgid())
        return;
    dbgprintf("%s: Send signal %d to everyone in pgrp %d\n", tty_name().characters(), signal, pgid());
    InterruptDisabler disabler; // FIXME: Iterate over a set of process handles instead?
    Process::for_each_in_pgrp(pgid(), [&] (auto& process) {
        dbgprintf("%s: Send signal %d to %d\n", tty_name().characters(), signal, process.pid());
        process.send_signal(signal, nullptr);
        return true;
    });
}

void TTY::set_termios(const termios& t)
{
    m_termios = t;
    dbgprintf("%s set_termios: ECHO=%u, ISIG=%u, ICANON=%u\n",
        tty_name().characters(),
        should_echo_input(),
        should_generate_signals(),
        in_canonical_mode()
    );
    dbgprintf("%s set_termios: ECHOE=%u, ECHOK=%u, ECHONL=%u\n",
              tty_name().characters(),
              (m_termios.c_lflag & ECHOE) != 0,
              (m_termios.c_lflag & ECHOK) != 0,
              (m_termios.c_lflag & ECHONL) != 0
              );
    dbgprintf("%s set_termios: ISTRIP=%u, ICRNL=%u, INLCR=%u, IGNCR=%u\n",
              tty_name().characters(),
              (m_termios.c_iflag & ISTRIP) != 0,
              (m_termios.c_iflag & ICRNL) != 0,
              (m_termios.c_iflag & INLCR) != 0,
              (m_termios.c_iflag & IGNCR) != 0
              );
}

int TTY::ioctl(FileDescriptor&, unsigned request, unsigned arg)
{
    auto& process = current->process();
    pid_t pgid;
    termios* tp;
    winsize* ws;

#if 0
    // FIXME: When should we block things?
    //        How do we make this work together with MasterPTY forwarding to us?
    if (process.tty() && process.tty() != this) {
        return -ENOTTY;
    }
#endif
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
        tp = reinterpret_cast<termios*>(arg);
        if (!process.validate_write(tp, sizeof(termios)))
            return -EFAULT;
        *tp = m_termios;
        return 0;
    case TCSETS:
    case TCSETSF:
    case TCSETSW:
        tp = reinterpret_cast<termios*>(arg);
        if (!process.validate_read(tp, sizeof(termios)))
            return -EFAULT;
        set_termios(*tp);
        return 0;
    case TIOCGWINSZ:
        ws = reinterpret_cast<winsize*>(arg);
        if (!process.validate_write(ws, sizeof(winsize)))
            return -EFAULT;
        ws->ws_row = m_rows;
        ws->ws_col = m_columns;
        return 0;
    case TIOCSWINSZ:
        ws = reinterpret_cast<winsize*>(arg);
        if (!process.validate_read(ws, sizeof(winsize)))
            return -EFAULT;
        if (ws->ws_col == m_columns && ws->ws_row == m_rows)
            return 0;
        m_rows = ws->ws_row;
        m_columns = ws->ws_col;
        generate_signal(SIGWINCH);
        return 0;
    case TIOCSCTTY:
        process.set_tty(this);
        return 0;
    case TIOCNOTTY:
        process.set_tty(nullptr);
        return 0;
    }
    ASSERT_NOT_REACHED();
    return -EINVAL;
}

void TTY::set_size(unsigned short columns, unsigned short rows)
{
    m_rows = rows;
    m_columns = columns;
}

void TTY::hang_up()
{
    generate_signal(SIGHUP);
}
