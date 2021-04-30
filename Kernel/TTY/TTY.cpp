/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <Kernel/Debug.h>
#include <Kernel/Process.h>
#include <Kernel/TTY/TTY.h>
#include <LibC/errno_numbers.h>
#include <LibC/signal_numbers.h>
#include <LibC/sys/ioctl_numbers.h>

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
    static const char default_cc[32] = "\003\034\010\025\004\0\1\0\021\023\032\0\022\017\027\026\0\024";
    memcpy(m_termios.c_cc, default_cc, sizeof(default_cc));
}

KResultOr<size_t> TTY::read(FileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    if (Process::current()->pgid() != pgid()) {
        // FIXME: Should we propagate this error path somehow?
        [[maybe_unused]] auto rc = Process::current()->send_signal(SIGTTIN, nullptr);
        return EINTR;
    }

    if (m_input_buffer.size() < static_cast<size_t>(size))
        size = m_input_buffer.size();

    ssize_t nwritten;
    bool need_evaluate_block_conditions = false;
    if (in_canonical_mode()) {
        nwritten = buffer.write_buffered<512>(size, [&](u8* data, size_t data_size) {
            size_t i = 0;
            for (; i < data_size; i++) {
                u8 ch = m_input_buffer.dequeue();
                if (ch == '\0') {
                    //Here we handle a ^D line, so we don't add the
                    //character to the output.
                    m_available_lines--;
                    need_evaluate_block_conditions = true;
                    break;
                } else if (ch == '\n' || is_eol(ch)) {
                    data[i] = ch;
                    i++;
                    m_available_lines--;
                    break;
                }
                data[i] = ch;
            }
            return (ssize_t)i;
        });
    } else {
        nwritten = buffer.write_buffered<512>(size, [&](u8* data, size_t data_size) {
            for (size_t i = 0; i < data_size; i++) {
                auto ch = m_input_buffer.dequeue();
                if (ch == '\r' && m_termios.c_iflag & ICRNL)
                    ch = '\n';
                else if (ch == '\n' && m_termios.c_iflag & INLCR)
                    ch = '\r';
                data[i] = ch;
            }
            return (ssize_t)data_size;
        });
    }
    if (nwritten < 0)
        return KResult((ErrnoCode)-nwritten);
    if (nwritten > 0 || need_evaluate_block_conditions)
        evaluate_block_conditions();
    return (size_t)nwritten;
}

KResultOr<size_t> TTY::write(FileDescription&, u64, const UserOrKernelBuffer& buffer, size_t size)
{
    if (m_termios.c_lflag & TOSTOP && Process::current()->pgid() != pgid()) {
        [[maybe_unused]] auto rc = Process::current()->send_signal(SIGTTOU, nullptr);
        return EINTR;
    }

    const size_t num_chars = 256;
    ssize_t nread = buffer.read_buffered<num_chars>((size_t)size, [&](const u8* data, size_t buffer_bytes) {
        u8 modified_data[num_chars * 2];
        size_t extra_chars = 0;
        for (size_t i = 0; i < buffer_bytes; ++i) {
            auto ch = data[i];
            if (ch == '\n' && (m_termios.c_oflag & (OPOST | ONLCR))) {
                modified_data[i + extra_chars] = '\r';
                extra_chars++;
            }
            modified_data[i + extra_chars] = ch;
        }
        on_tty_write(UserOrKernelBuffer::for_kernel_buffer(modified_data), buffer_bytes + extra_chars);
        return (ssize_t)buffer_bytes;
    });

    return nread;
}

bool TTY::can_read(const FileDescription&, size_t) const
{
    if (in_canonical_mode()) {
        return m_available_lines > 0;
    }
    return !m_input_buffer.is_empty();
}

bool TTY::can_write(const FileDescription&, size_t) const
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

void TTY::emit(u8 ch, bool do_evaluate_block_conditions)
{
    if (should_generate_signals()) {
        if (ch == m_termios.c_cc[VINFO]) {
            dbgln("{}: VINFO pressed!", tty_name());
            generate_signal(SIGINFO);
            return;
        }
        if (ch == m_termios.c_cc[VINTR]) {
            dbgln("{}: VINTR pressed!", tty_name());
            generate_signal(SIGINT);
            return;
        }
        if (ch == m_termios.c_cc[VQUIT]) {
            dbgln("{}: VQUIT pressed!", tty_name());
            generate_signal(SIGQUIT);
            return;
        }
        if (ch == m_termios.c_cc[VSUSP]) {
            dbgln("{}: VSUSP pressed!", tty_name());
            generate_signal(SIGTSTP);
            if (auto original_process_parent = m_original_process_parent.strong_ref()) {
                [[maybe_unused]] auto rc = original_process_parent->send_signal(SIGCHLD, nullptr);
            }
            // TODO: Else send it to the session leader maybe?
            return;
        }
    }

    ScopeGuard guard([&]() {
        if (do_evaluate_block_conditions)
            evaluate_block_conditions();
    });

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
    // can't do back space if we're empty. Plus, we don't want to
    // remove any lines "committed" by newlines or ^D.
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

        evaluate_block_conditions();
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
    bool did_dequeue = false;
    while (can_do_backspace()) {
        u8 ch = m_input_buffer.last();
        if (ch == ' ' && first_char)
            break;
        if (ch != ' ')
            first_char = true;
        m_input_buffer.dequeue_end();
        did_dequeue = true;
        erase_character();
    }
    if (did_dequeue)
        evaluate_block_conditions();
}

void TTY::kill_line()
{
    bool did_dequeue = false;
    while (can_do_backspace()) {
        m_input_buffer.dequeue_end();
        did_dequeue = true;
        erase_character();
    }
    if (did_dequeue)
        evaluate_block_conditions();
}

void TTY::erase_character()
{
    echo(m_termios.c_cc[VERASE]);
    echo(' ');
    echo(m_termios.c_cc[VERASE]);
}

void TTY::generate_signal(int signal)
{
    if (!pgid())
        return;
    if (should_flush_on_signal())
        flush_input();
    dbgln_if(TTY_DEBUG, "{}: Send signal {} to everyone in pgrp {}", tty_name(), signal, pgid().value());
    InterruptDisabler disabler; // FIXME: Iterate over a set of process handles instead?
    Process::for_each_in_pgrp(pgid(), [&](auto& process) {
        dbgln_if(TTY_DEBUG, "{}: Send signal {} to {}", tty_name(), signal, process);
        // FIXME: Should this error be propagated somehow?
        [[maybe_unused]] auto rc = process.send_signal(signal, nullptr);
        return IterationDecision::Continue;
    });
}

void TTY::flush_input()
{
    m_available_lines = 0;
    m_input_buffer.clear();
    evaluate_block_conditions();
}

void TTY::set_termios(const termios& t)
{
    m_termios = t;

    dbgln_if(TTY_DEBUG, "{} set_termios: ECHO={}, ISIG={}, ICANON={}, ECHOE={}, ECHOK={}, ECHONL={}, ISTRIP={}, ICRNL={}, INLCR={}, IGNCR={}, OPOST={}, ONLCR={}",
        tty_name(),
        should_echo_input(),
        should_generate_signals(),
        in_canonical_mode(),
        ((m_termios.c_lflag & ECHOE) != 0),
        ((m_termios.c_lflag & ECHOK) != 0),
        ((m_termios.c_lflag & ECHONL) != 0),
        ((m_termios.c_iflag & ISTRIP) != 0),
        ((m_termios.c_iflag & ICRNL) != 0),
        ((m_termios.c_iflag & INLCR) != 0),
        ((m_termios.c_iflag & IGNCR) != 0),
        ((m_termios.c_oflag & OPOST) != 0),
        ((m_termios.c_oflag & ONLCR) != 0));
}

int TTY::ioctl(FileDescription&, unsigned request, FlatPtr arg)
{
    REQUIRE_PROMISE(tty);
    auto& current_process = *Process::current();
    termios* user_termios;
    winsize* user_winsize;

#if 0
    // FIXME: When should we block things?
    //        How do we make this work together with MasterPTY forwarding to us?
    if (current_process.tty() && current_process.tty() != this) {
        return -ENOTTY;
    }
#endif
    switch (request) {
    case TIOCGPGRP:
        return this->pgid().value();
    case TIOCSPGRP: {
        ProcessGroupID pgid = static_cast<pid_t>(arg);
        if (pgid <= 0)
            return -EINVAL;
        InterruptDisabler disabler;
        auto process_group = ProcessGroup::from_pgid(pgid);
        // Disallow setting a nonexistent PGID.
        if (!process_group)
            return -EINVAL;

        auto process = Process::from_pid(ProcessID(pgid.value()));
        SessionID new_sid = process ? process->sid() : Process::get_sid_from_pgid(pgid);
        if (!new_sid || new_sid != current_process.sid())
            return -EPERM;
        if (process && pgid != process->pgid())
            return -EPERM;
        m_pg = process_group;

        if (process) {
            if (auto parent = Process::from_pid(process->ppid())) {
                m_original_process_parent = *parent;
                return 0;
            }
        }

        m_original_process_parent = nullptr;
        return 0;
    }
    case TCGETS: {
        user_termios = reinterpret_cast<termios*>(arg);
        if (!copy_to_user(user_termios, &m_termios))
            return -EFAULT;
        return 0;
    }
    case TCSETS:
    case TCSETSF:
    case TCSETSW: {
        user_termios = reinterpret_cast<termios*>(arg);
        termios termios;
        if (!copy_from_user(&termios, user_termios))
            return -EFAULT;
        set_termios(termios);
        if (request == TCSETSF)
            flush_input();
        return 0;
    }
    case TCFLSH:
        // Serenity's TTY implementation does not use an output buffer, so ignore TCOFLUSH.
        if (arg == TCIFLUSH || arg == TCIOFLUSH) {
            flush_input();
        } else if (arg != TCOFLUSH) {
            return -EINVAL;
        }
        return 0;
    case TIOCGWINSZ:
        user_winsize = reinterpret_cast<winsize*>(arg);
        winsize ws;
        ws.ws_row = m_rows;
        ws.ws_col = m_columns;
        ws.ws_xpixel = 0;
        ws.ws_ypixel = 0;
        if (!copy_to_user(user_winsize, &ws))
            return -EFAULT;
        return 0;
    case TIOCSWINSZ: {
        user_winsize = reinterpret_cast<winsize*>(arg);
        winsize ws;
        if (!copy_from_user(&ws, user_winsize))
            return -EFAULT;
        if (ws.ws_col == m_columns && ws.ws_row == m_rows)
            return 0;
        m_rows = ws.ws_row;
        m_columns = ws.ws_col;
        generate_signal(SIGWINCH);
        return 0;
    }
    case TIOCSCTTY:
        current_process.set_tty(this);
        return 0;
    case TIOCSTI:
        return -EIO;
    case TIOCNOTTY:
        current_process.set_tty(nullptr);
        return 0;
    }
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
