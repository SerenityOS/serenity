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

#include "Process.h"
#include <Kernel/TTY/TTY.h>
#include <LibC/errno_numbers.h>
#include <LibC/signal_numbers.h>
#include <LibC/sys/ioctl_numbers.h>

//#define TTY_DEBUG

namespace Kernel {

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
    m_termios.c_lflag |= ISIG | ECHO | ICANON;
    static const char default_cc[32] = "\003\034\010\025\004\0\1\0\021\023\032\0\022\017\027\026\0";
    memcpy(m_termios.c_cc, default_cc, sizeof(default_cc));
}

ssize_t TTY::read(FileDescription&, u8* buffer, ssize_t size)
{
    if (m_input_buffer.size() < size)
        size = m_input_buffer.size();

    if (in_canonical_mode()) {
        int i = 0;
        for (; i < size; i++) {
            u8 ch = m_input_buffer.dequeue();
            if (ch == '\0') {
                //Here we handle a ^D line, so we don't add the
                //character to the output.
                m_available_lines--;
                break;
            } else if (ch == '\n' || is_eol(ch)) {
                buffer[i] = ch;
                i++;
                m_available_lines--;
                break;
            }
            buffer[i] = ch;
        }
        return i;
    }

    for (int i = 0; i < size; i++)
        buffer[i] = m_input_buffer.dequeue();

    return size;
}

ssize_t TTY::write(FileDescription&, const u8* buffer, ssize_t size)
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

bool TTY::can_read(const FileDescription&) const
{
    if (in_canonical_mode()) {
        return m_available_lines > 0;
    }
    return !m_input_buffer.is_empty();
}

bool TTY::can_write(const FileDescription&) const
{
    return true;
}

bool TTY::is_eol(u8 ch) const
{
    return ch == m_termios.c_cc[VEOL];
}

bool TTY::is_eof(u8 ch) const
{
    return ch == m_termios.c_cc[VEOF];
}

bool TTY::is_kill(u8 ch) const
{
    return ch == m_termios.c_cc[VKILL];
}

bool TTY::is_erase(u8 ch) const
{
    return ch == m_termios.c_cc[VERASE];
}

bool TTY::is_werase(u8 ch) const
{
    return ch == m_termios.c_cc[VWERASE];
}

void TTY::emit(u8 ch)
{
    if (should_generate_signals()) {
        if (ch == m_termios.c_cc[VINTR]) {
            dbg() << tty_name() << ": VINTR pressed!";
            generate_signal(SIGINT);
            return;
        }
        if (ch == m_termios.c_cc[VQUIT]) {
            dbg() << tty_name() << ": VQUIT pressed!";
            generate_signal(SIGQUIT);
            return;
        }
        if (ch == m_termios.c_cc[VSUSP]) {
            dbg() << tty_name() << ": VSUSP pressed!";
            generate_signal(SIGTSTP);
            return;
        }
    }

    if (in_canonical_mode()) {
        if (is_eof(ch)) {
            m_available_lines++;
            //We use '\0' to delimit the end
            //of a line.
            m_input_buffer.enqueue('\0');
            return;
        }
        if (is_kill(ch)) {
            kill_line();
            return;
        }
        if (is_erase(ch)) {
            do_backspace();
            return;
        }
        if (is_werase(ch)) {
            erase_word();
            return;
        }
        if (ch == '\n' || is_eol(ch)) {
            m_available_lines++;
        }
    }
    m_input_buffer.enqueue(ch);
    echo(ch);
}

bool TTY::can_do_backspace() const
{
    //can't do back space if we're empty. Plus, we don't want to
    //removing any lines "commited" by newlines or ^D.
    if (!m_input_buffer.is_empty() && !is_eol(m_input_buffer.last()) && m_input_buffer.last() != '\0') {
        return true;
    }
    return false;
}

void TTY::do_backspace()
{
    if (can_do_backspace()) {
        m_input_buffer.dequeue_end();
        echo(8);
        echo(' ');
        echo(8);
    }
}

// TODO: Currently, both erase_word() and kill_line work by sending
// a lot of VERASE characters; this is done because Terminal.cpp
// doesn't currently support VWERASE and VKILL. When these are
// implemented we could just send a VKILL or VWERASE.

void TTY::erase_word()
{
    //Note: if we have leading whitespace before the word
    //we want to delete we have to also delete that.
    bool first_char = false;
    while (can_do_backspace()) {
        u8 ch = m_input_buffer.last();
        if (ch == ' ' && first_char)
            break;
        if (ch != ' ')
            first_char = true;
        m_input_buffer.dequeue_end();
        echo(m_termios.c_cc[VERASE]);
    }
}

void TTY::kill_line()
{
    while (can_do_backspace()) {
        m_input_buffer.dequeue_end();
        echo(m_termios.c_cc[VERASE]);
    }
}

void TTY::generate_signal(int signal)
{
    if (!pgid())
        return;
    if (should_flush_on_signal())
        flush_input();
    dbg() << tty_name() << ": Send signal " << signal << " to everyone in pgrp " << pgid();
    InterruptDisabler disabler; // FIXME: Iterate over a set of process handles instead?
    Process::for_each_in_pgrp(pgid(), [&](auto& process) {
        dbg() << tty_name() << ": Send signal " << signal << " to " << process;
        process.send_signal(signal, nullptr);
        return IterationDecision::Continue;
    });
}

void TTY::flush_input()
{
    m_available_lines = 0;
    m_input_buffer.clear();
}

void TTY::set_termios(const termios& t)
{
    m_termios = t;
#ifdef TTY_DEBUG
    dbg() << tty_name() << " set_termios: "
          << "ECHO=" << should_echo_input()
          << ", ISIG=" << should_generate_signals()
          << ", ICANON=" << in_canonical_mode()
          << ", ECHOE=" << ((m_termios.c_lflag & ECHOE) != 0)
          << ", ECHOK=" << ((m_termios.c_lflag & ECHOK) != 0)
          << ", ECHONL=" << ((m_termios.c_lflag & ECHONL) != 0)
          << ", ISTRIP=" << ((m_termios.c_iflag & ISTRIP) != 0)
          << ", ICRNL=" << ((m_termios.c_iflag & ICRNL) != 0)
          << ", INLCR=" << ((m_termios.c_iflag & INLCR) != 0)
          << ", IGNCR=" << ((m_termios.c_iflag & IGNCR) != 0);
#endif
}

int TTY::ioctl(FileDescription&, unsigned request, unsigned arg)
{
    REQUIRE_PROMISE(tty);
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

}
