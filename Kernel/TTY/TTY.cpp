/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <AK/StringView.h>
#include <Kernel/Arch/x86/InterruptDisabler.h>
#include <Kernel/Debug.h>
#include <Kernel/Process.h>
#include <Kernel/TTY/TTY.h>
#include <LibC/errno_numbers.h>
#include <LibC/signal_numbers.h>
#include <LibC/sys/ioctl_numbers.h>
#define TTYDEFCHARS
#include <LibC/sys/ttydefaults.h>
#undef TTYDEFCHARS

namespace Kernel {

TTY::TTY(unsigned major, unsigned minor)
    : CharacterDevice(major, minor)
{
}

TTY::~TTY()
{
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

    bool need_evaluate_block_conditions = false;
    auto result = buffer.write_buffered<512>(size, [&](u8* data, size_t data_size) {
        size_t bytes_written = 0;
        for (; bytes_written < data_size; ++bytes_written) {
            if (in_canonical_mode() && is_special_character_at(m_input_buffer.head_index())) {
                u8 ch = m_input_buffer.dequeue();
                if (ch == '\0') {
                    // EOF
                    m_available_lines--;
                    need_evaluate_block_conditions = true;
                    break;
                } else {
                    // '\n' or EOL
                    data[bytes_written++] = ch;
                    m_available_lines--;
                    break;
                }
            }
            data[bytes_written] = m_input_buffer.dequeue();
        }
        return bytes_written;
    });
    if ((!result.is_error() && result.value() > 0) || need_evaluate_block_conditions)
        evaluate_block_conditions();
    return result;
}

KResultOr<size_t> TTY::write(FileDescription&, u64, const UserOrKernelBuffer& buffer, size_t size)
{
    if (m_termios.c_lflag & TOSTOP && Process::current()->pgid() != pgid()) {
        [[maybe_unused]] auto rc = Process::current()->send_signal(SIGTTOU, nullptr);
        return EINTR;
    }

    constexpr size_t num_chars = 256;
    return buffer.read_buffered<num_chars>(size, [&](u8 const* data, size_t buffer_bytes) -> KResultOr<size_t> {
        u8 modified_data[num_chars * 2];
        size_t modified_data_size = 0;
        for (size_t i = 0; i < buffer_bytes; ++i) {
            process_output(data[i], [&modified_data, &modified_data_size](u8 out_ch) {
                modified_data[modified_data_size++] = out_ch;
            });
        }
        auto bytes_written_or_error = on_tty_write(UserOrKernelBuffer::for_kernel_buffer(modified_data), modified_data_size);
        if (bytes_written_or_error.is_error() || !(m_termios.c_oflag & OPOST) || !(m_termios.c_oflag & ONLCR))
            return bytes_written_or_error;
        auto bytes_written = bytes_written_or_error.value();
        if (bytes_written == modified_data_size)
            return buffer_bytes;

        // Degenerate case where we converted some newlines and encountered a partial write

        // Calculate where in the input buffer the last character would have been
        size_t pos_data = 0;
        for (size_t pos_modified_data = 0; pos_modified_data < bytes_written; ++pos_data) {
            if (data[pos_data] == '\n')
                pos_modified_data += 2;
            else
                pos_modified_data += 1;

            // Handle case where the '\r' got written but not the '\n'
            // FIXME: Our strategy is to retry writing both. We should really be queuing a write for the corresponding '\n'
            if (pos_modified_data > bytes_written)
                --pos_data;
        }
        return pos_data;
    });
}

void TTY::echo_with_processing(u8 ch)
{
    process_output(ch, [this](u8 out_ch) { echo(out_ch); });
}

template<typename Functor>
void TTY::process_output(u8 ch, Functor put_char)
{
    if (m_termios.c_oflag & OPOST) {
        if (ch == '\n' && (m_termios.c_oflag & ONLCR))
            put_char('\r');
        put_char(ch);
    } else {
        put_char(ch);
    }
}

bool TTY::can_read(const FileDescription&, size_t) const
{
    if (in_canonical_mode()) {
        return m_available_lines > 0;
    } else {
        return !m_input_buffer.is_empty();
    }
}

bool TTY::can_write(const FileDescription&, size_t) const
{
    return true;
}

constexpr static bool is_utf8_continuation_byte(u8 byte)
{
    return (byte & 0xc0) == 0x80;
}

bool TTY::should_skip_erasing_byte(u8 ch) const
{
    return (m_termios.c_iflag & IUTF8) && is_utf8_continuation_byte(ch);
}

bool TTY::is_eol(u8 ch) const
{
    return ch == m_termios.c_cc[VEOL];
}

bool TTY::is_eol2(u8 ch) const
{
    return extended_processing_enabled() && ch == m_termios.c_cc[VEOL2];
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
    return extended_processing_enabled() && ch == m_termios.c_cc[VWERASE];
}

void TTY::emit(u8 ch, bool do_evaluate_block_conditions)
{
    // FIXME: add support for parity checking once we have serial TTYs.
    //        0xFF bytes that escape parity errors should not be stripped.
    if (m_termios.c_iflag & ISTRIP)
        ch &= 0x7F;

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

    if (ch == '\r' && (m_termios.c_iflag & ICRNL))
        ch = '\n';
    else if (ch == '\n' && (m_termios.c_iflag & INLCR))
        ch = '\r';

    auto current_char_head_index = (m_input_buffer.head_index() + m_input_buffer.size()) % TTY_BUFFER_SIZE;
    m_special_character_bitmask[current_char_head_index / 8] &= ~(1u << (current_char_head_index % 8));

    auto set_special_bit = [&] {
        m_special_character_bitmask[current_char_head_index / 8] |= (1u << (current_char_head_index % 8));
    };

    if (in_canonical_mode()) {
        if (is_eof(ch)) {
            // Since EOF might change between when the data came in and when it is read,
            // we use '\0' along with the bitmask to signal EOF. Any non-zero byte with
            // the special bit set signals an end-of-line.
            set_special_bit();
            m_available_lines++;
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

        if (ch == '\n') {
            if (m_termios.c_lflag & ECHO || m_termios.c_lflag & ECHONL)
                echo_with_processing('\n');

            set_special_bit();
            m_input_buffer.enqueue('\n');
            m_available_lines++;
            return;
        }

        if (is_eol(ch) || is_eol2(ch)) {
            set_special_bit();
            m_available_lines++;
        }
    }

    m_input_buffer.enqueue(ch);
    if (m_termios.c_lflag & ECHO)
        echo_with_processing(ch);
}

bool TTY::can_do_backspace() const
{
    // Can't do backspace if we're empty. Plus, we don't want to
    // remove any lines "committed" by newlines or ^D.
    size_t tail_index = (m_input_buffer.head_index() + m_input_buffer.size() - 1) % TTY_BUFFER_SIZE;
    return !m_input_buffer.is_empty() && !is_special_character_at(tail_index);
}

// Erase a character by removing it from the input buffer.
// If ECHOE is set, a space surrounded '\b' characters is echoed.
// Otherwise, the ERASE character is echoed.
void TTY::do_backspace()
{
    // If we have a multibyte UTF-8 sequence, we want to remove all bytes of it
    // from the buffer, but only want to emit a single erase character.
    while (can_do_backspace() && should_skip_erasing_byte(m_input_buffer.last()))
        m_input_buffer.dequeue_end();

    if (!can_do_backspace())
        return;

    m_input_buffer.dequeue_end();
    if (m_termios.c_lflag & ECHOE)
        erase_character();
    else
        echo(m_termios.c_cc[VERASE]);
    evaluate_block_conditions();
}

// Erase a word by removing it from the input buffer.
// The echoing works as in do_backspace().
void TTY::erase_word()
{
    // If we have leading whitespace before the word, we have to also delete that.
    bool is_trailing_whitespace = true;
    bool did_dequeue = false;
    while (can_do_backspace()) {
        u8 ch = m_input_buffer.last();
        if (ch == ' ' && !is_trailing_whitespace)
            break;
        if (ch != ' ')
            is_trailing_whitespace = false;
        m_input_buffer.dequeue_end();
        did_dequeue = true;

        // Handle multibyte UTF-8 sequences correctly.
        if (!should_skip_erasing_byte(ch)) {
            if (m_termios.c_lflag & ECHOE)
                erase_character();
            else
                echo(m_termios.c_cc[VERASE]);
        }
    }
    if (did_dequeue)
        evaluate_block_conditions();
}

// Erase an entire line of input by removing it from the input buffer.
// If ECHOKE is set, a space surrounded by '\b' is emitted for each character we delete.
// If ECHOK is set, the ECHOK is emitted once, followed by a newline.
void TTY::kill_line()
{
    bool did_dequeue = false;
    while (can_do_backspace()) {
        u8 ch = m_input_buffer.dequeue_end();
        did_dequeue = true;
        if (!should_skip_erasing_byte(ch) && m_termios.c_lflag & ECHOKE)
            erase_character();
    }

    if (!(m_termios.c_lflag & ECHOKE)) {
        echo(m_termios.c_cc[VKILL]);
        if (m_termios.c_lflag & ECHOK)
            echo_with_processing('\n');
    }
    if (did_dequeue)
        evaluate_block_conditions();
}

void TTY::erase_character()
{
    // We deliberately don't process the output here.
    echo('\b');
    echo(' ');
    echo('\b');
}

void TTY::generate_signal(int signal)
{
    if (!pgid())
        return;
    if (should_flush_on_signal()) {
        flush_input();
        flush_output();
    }
    dbgln_if(TTY_DEBUG, "{}: Send signal {} to everyone in pgrp {}", tty_name(), signal, pgid().value());
    InterruptDisabler disabler; // FIXME: Iterate over a set of process handles instead?
    Process::for_each_in_pgrp(pgid(), [&](auto& process) {
        dbgln_if(TTY_DEBUG, "{}: Send signal {} to {}", tty_name(), signal, process);
        // FIXME: Should this error be propagated somehow?
        [[maybe_unused]] auto rc = process.send_signal(signal, nullptr);
    });
}

// In this context, flush means to discard any unprocessed data.

void TTY::flush_input()
{
    m_available_lines = 0;
    m_input_buffer.clear();
    discard_input_buffer();
    evaluate_block_conditions();
}

void TTY::flush_output()
{
    discard_output_buffer();
}

// Subclasses can call this once they're ready to setup the various serial parameters.
void TTY::reload_termios()
{
    set_termios(m_termios, true);
}

int TTY::set_termios(const termios& new_termios, bool force_set)
{
    int rc = 0;

    // We intentionally do not return early from this function, so as to keep programs in a (somewhat) working state
    // even if one of the options it sets isn't implemented.
    auto attempt = [&](int call_rc) {
        if (call_rc < 0)
            rc = call_rc;
    };

    dbgln_if(TTY_DEBUG, "{} set_termios: ECHO={}, ISIG={}, ICANON={}, ECHOE={}, ECHOK={}, ECHONL={}, ISTRIP={}, ICRNL={}, INLCR={}, IGNCR={}, OPOST={}, ONLCR={}",
        tty_name(),
        should_echo_input(),
        should_generate_signals(),
        in_canonical_mode(),
        ((new_termios.c_lflag & ECHOE) != 0),
        ((new_termios.c_lflag & ECHOK) != 0),
        ((new_termios.c_lflag & ECHONL) != 0),
        ((new_termios.c_iflag & ISTRIP) != 0),
        ((new_termios.c_iflag & ICRNL) != 0),
        ((new_termios.c_iflag & INLCR) != 0),
        ((new_termios.c_iflag & IGNCR) != 0),
        ((new_termios.c_oflag & OPOST) != 0),
        ((new_termios.c_oflag & ONLCR) != 0));

    // We deliberately don't return early for unimplemented flags, so that we can get all debug messages
    struct FlagDescription {
        tcflag_t value;
        StringView name;
    };

    static constexpr FlagDescription unimplemented_iflags[] = {
        { BRKINT, "BRKINT" },
        { PARMRK, "PARMRK" },
        { INPCK, "INPCK" },
        { IGNCR, "IGNCR" },
        { IUCLC, "IUCLC" },
        { IXON, "IXON" },
        { IXANY, "IXANY" },
        { IXOFF, "IXOFF" },
        { IMAXBEL, "IMAXBEL" }
    };
    for (auto flag : unimplemented_iflags) {
        if (new_termios.c_iflag & flag.value) {
            dbgln("FIXME: iflag {} unimplemented", flag.name);
            rc = -ENOTIMPL;
        }
    }

    static constexpr FlagDescription unimplemented_oflags[] = {
        { OLCUC, "OLCUC" },
        { ONOCR, "ONOCR" },
        { ONLRET, "ONLRET" },
        { OFILL, "OFILL" },
        { OFDEL, "OFDEL" }
    };
    for (auto flag : unimplemented_oflags) {
        if (new_termios.c_oflag & flag.value) {
            dbgln("FIXME: oflag {} unimplemented", flag.name);
            rc = -ENOTIMPL;
        }
    }

    static constexpr FlagDescription unimplemented_cflags[] = {
        { HUPCL, "HUPCL" }
    };
    for (auto flag : unimplemented_cflags) {
        if (new_termios.c_cflag & flag.value) {
            dbgln("FIXME: cflag {} unimplemented", flag.name);
            rc = -ENOTIMPL;
        }
    }

    if (force_set || new_termios.c_ispeed != m_termios.c_ispeed || new_termios.c_ospeed != m_termios.c_ospeed)
        attempt(change_baud(new_termios.c_ispeed, new_termios.c_ospeed));

    static_assert(__builtin_popcount(CSIZE) == 2, "There are 4 character sizes, so CSIZE should be a 2-bit bitmask.");
    static_assert((CS5 & ~CSIZE) == 0, "CS5 must be contained within the CSIZE bitmask.");
    static_assert((CS6 & ~CSIZE) == 0, "CS6 must be contained within the CSIZE bitmask.");
    static_assert((CS7 & ~CSIZE) == 0, "CS7 must be contained within the CSIZE bitmask.");
    static_assert((CS8 & ~CSIZE) == 0, "CS8 must be contained within the CSIZE bitmask.");

    if (force_set || (new_termios.c_cflag & CSIZE) != (m_termios.c_cflag & CSIZE)) {
        switch (new_termios.c_cflag & CSIZE) {
        case CS5:
            attempt(change_character_size(CharacterSize::FiveBits));
            break;
        case CS6:
            attempt(change_character_size(CharacterSize::SixBits));
            break;
        case CS7:
            attempt(change_character_size(CharacterSize::SevenBits));
            break;
        case CS8:
            attempt(change_character_size(CharacterSize::EightBits));
            break;
        }
    }

    if (force_set || (new_termios.c_cflag & CSTOPB) != (m_termios.c_cflag & CSTOPB)) {
        if (new_termios.c_cflag & CSTOPB)
            attempt(change_stop_bits(StopBits::Two));
        else
            attempt(change_stop_bits(StopBits::One));
    }

    if (force_set || (new_termios.c_cflag & CREAD) != (m_termios.c_cflag & CREAD))
        attempt(change_receiver_enabled(new_termios.c_cflag & CREAD));

    if (force_set || (new_termios.c_cflag & PARENB) != (m_termios.c_cflag & PARENB) || (new_termios.c_cflag & PARODD) != (m_termios.c_cflag & PARODD)) {
        if (!(new_termios.c_cflag & PARENB))
            attempt(change_parity(Parity::None));
        else if (new_termios.c_cflag & PARODD)
            attempt(change_parity(Parity::Odd));
        else
            attempt(change_parity(Parity::Even));
        // FIXME: Add sticky parity handling (not POSIX, but other systems support it).
    }

    if (force_set || (new_termios.c_cflag & CLOCAL) != (m_termios.c_cflag & CLOCAL))
        attempt(change_ignore_modem_status(new_termios.c_cflag & CLOCAL));

    m_termios = new_termios;
    return rc;
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
        int rc = set_termios(termios);
        if (request == TCSETSF)
            flush_input();
        return rc;
    }
    case TCFLSH:
        if (arg == TCIOFLUSH) {
            flush_input();
            flush_output();
        } else if (arg == TCIFLUSH) {
            flush_input();
        } else if (arg == TCOFLUSH) {
            flush_output();
        } else {
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
