/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <AK/StringView.h>
#include <Kernel/API/Ioctl.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/API/POSIX/signal_numbers.h>
#include <Kernel/API/ttydefaults.h>
#include <Kernel/API/ttydefaultschars.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/TTY/TTY.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

TTY::TTY(MajorAllocation::CharacterDeviceFamily character_device_family, MinorNumber minor)
    : CharacterDevice(character_device_family, minor)
{
    set_default_termios();
}

TTY::~TTY() = default;

void TTY::set_default_termios()
{
    memset(&m_termios, 0, sizeof(m_termios));
    m_termios.c_iflag = TTYDEF_IFLAG;
    m_termios.c_oflag = TTYDEF_OFLAG;
    m_termios.c_cflag = TTYDEF_CFLAG;
    m_termios.c_lflag = TTYDEF_LFLAG;
    m_termios.c_ispeed = TTYDEF_SPEED;
    m_termios.c_ospeed = TTYDEF_SPEED;
    memcpy(m_termios.c_cc, ttydefchars, sizeof(ttydefchars));
}

ErrorOr<size_t> TTY::read(OpenFileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    if (Process::current().pgid() != pgid()) {
        // FIXME: Should we propagate this error path somehow?
        [[maybe_unused]] auto rc = Process::current().send_signal(SIGTTIN, nullptr);
        return EINTR;
    }
    if (m_input_buffer.size() < static_cast<size_t>(size))
        size = m_input_buffer.size();

    bool need_evaluate_block_conditions = false;
    auto result = buffer.write_buffered<512>(size, [&](Bytes data) {
        size_t bytes_written = 0;
        for (; bytes_written < data.size(); ++bytes_written) {
            auto bit_index = m_input_buffer.head_index();
            bool is_special_character = m_special_character_bitmask[bit_index / 8] & (1 << (bit_index % 8));
            if (in_canonical_mode() && is_special_character) {
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

ErrorOr<size_t> TTY::write(OpenFileDescription&, u64, UserOrKernelBuffer const& buffer, size_t size)
{
    if (m_termios.c_lflag & TOSTOP && Process::current().pgid() != pgid()) {
        [[maybe_unused]] auto rc = Process::current().send_signal(SIGTTOU, nullptr);
        return EINTR;
    }

    constexpr size_t num_chars = 256;
    return buffer.read_buffered<num_chars>(size, [&](ReadonlyBytes bytes) -> ErrorOr<size_t> {
        u8 modified_data[num_chars * 2];
        size_t modified_data_size = 0;
        for (auto const& byte : bytes) {
            process_output(byte, [&modified_data, &modified_data_size](u8 out_ch) {
                modified_data[modified_data_size++] = out_ch;
            });
        }
        auto bytes_written_or_error = on_tty_write(UserOrKernelBuffer::for_kernel_buffer(modified_data), modified_data_size);
        if (bytes_written_or_error.is_error() || !(m_termios.c_oflag & OPOST) || !(m_termios.c_oflag & ONLCR))
            return bytes_written_or_error;
        auto bytes_written = bytes_written_or_error.value();
        if (bytes_written == modified_data_size)
            return bytes.size();

        // Degenerate case where we converted some newlines and encountered a partial write

        // Calculate where in the input buffer the last character would have been
        size_t pos_data = 0;
        for (size_t pos_modified_data = 0; pos_modified_data < bytes_written; ++pos_data) {
            if (bytes[pos_data] == '\n')
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

bool TTY::can_read(OpenFileDescription const&, u64) const
{
    if (in_canonical_mode()) {
        return m_available_lines > 0;
    }
    return !m_input_buffer.is_empty();
}

bool TTY::can_write(OpenFileDescription const&, u64) const
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
    if (m_termios.c_iflag & ISTRIP)
        ch &= 0x7F;

    if (should_generate_signals()) {
        if (ch == m_termios.c_cc[VINFO]) {
            generate_signal(SIGINFO);
            return;
        }
        if (ch == m_termios.c_cc[VINTR]) {
            generate_signal(SIGINT);
            return;
        }
        if (ch == m_termios.c_cc[VQUIT]) {
            generate_signal(SIGQUIT);
            return;
        }
        if (ch == m_termios.c_cc[VSUSP]) {
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
        if (is_kill(ch) && m_termios.c_lflag & ECHOK) {
            kill_line();
            return;
        }
        if (is_erase(ch) && m_termios.c_lflag & ECHOE) {
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

        if (is_eol(ch)) {
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
    // can't do back space if we're empty. Plus, we don't want to
    // remove any lines "committed" by newlines or ^D.
    if (!m_input_buffer.is_empty() && !is_eol(m_input_buffer.last()) && m_input_buffer.last() != '\0') {
        return true;
    }
    return false;
}

static size_t length_with_tabs(CircularDeque<u8, TTY_BUFFER_SIZE> const& line)
{
    size_t length = 0;
    for (auto& ch : line) {
        length += (ch == '\t') ? 8 - (length % 8) : 1;
    }
    return length;
}

void TTY::do_backspace()
{
    if (can_do_backspace()) {
        auto ch = m_input_buffer.dequeue_end();
        size_t to_delete = 1;

        if (ch == '\t') {
            auto length = length_with_tabs(m_input_buffer);
            to_delete = 8 - (length % 8);
        }

        for (size_t i = 0; i < to_delete; ++i) {
            // We deliberately don't process the output here.
            echo('\b');
            echo(' ');
            echo('\b');
        }

        evaluate_block_conditions();
    }
}

// TODO: Currently, both erase_word() and kill_line work by sending
// a lot of VERASE characters; this is done because Terminal.cpp
// doesn't currently support VWERASE and VKILL. When these are
// implemented we could just send a VKILL or VWERASE.

void TTY::erase_word()
{
    // Note: if we have leading whitespace before the word
    // we want to delete we have to also delete that.
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
    // We deliberately don't process the output here.
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
    dbgln_if(TTY_DEBUG, "Send signal {} to everyone in pgrp {}", signal, pgid().value());
    InterruptDisabler disabler; // FIXME: Iterate over a set of process handles instead?
    MUST(Process::current().for_each_in_pgrp_in_same_process_list(pgid(), [&](auto& process) -> ErrorOr<void> {
        dbgln_if(TTY_DEBUG, "Send signal {} to {}", signal, process);
        // FIXME: Should this error be propagated somehow?
        [[maybe_unused]] auto rc = process.send_signal(signal, nullptr);
        return {};
    }));
}

void TTY::flush_input()
{
    m_available_lines = 0;
    m_input_buffer.clear();
    evaluate_block_conditions();
}

ErrorOr<void> TTY::set_termios(OpenFileDescription& description, termios const& t)
{
    ErrorOr<void> rc;
    m_termios = t;

    dbgln_if(TTY_DEBUG, "set_termios: ECHO={}, ISIG={}, ICANON={}, ECHOE={}, ECHOK={}, ECHONL={}, ISTRIP={}, ICRNL={}, INLCR={}, IGNCR={}, OPOST={}, ONLCR={}",
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

    struct FlagDescription {
        tcflag_t value;
        StringView name;
    };

    constexpr FlagDescription unimplemented_iflags[] = {
        { IGNBRK, "IGNBRK"sv },
        { BRKINT, "BRKINT"sv },
        { IGNPAR, "IGNPAR"sv },
        { PARMRK, "PARMRK"sv },
        { INPCK, "INPCK"sv },
        { IGNCR, "IGNCR"sv },
        { IUCLC, "IUCLC"sv },
        { IXON, "IXON"sv },
        { IXANY, "IXANY"sv },
        { IXOFF, "IXOFF"sv },
        { IMAXBEL, "IMAXBEL"sv },
        { IUTF8, "IUTF8"sv }
    };
    for (auto flag : unimplemented_iflags) {
        if (m_termios.c_iflag & flag.value) {
            dbgln("FIXME: iflag {} unimplemented", flag.name);
            rc = ENOTIMPL;
        }
    }

    constexpr FlagDescription unimplemented_oflags[] = {
        { OLCUC, "OLCUC"sv },
        { ONOCR, "ONOCR"sv },
        { ONLRET, "ONLRET"sv },
        { OFILL, "OFILL"sv },
        { OFDEL, "OFDEL"sv }
    };
    for (auto flag : unimplemented_oflags) {
        if (m_termios.c_oflag & flag.value) {
            dbgln("FIXME: oflag {} unimplemented", flag.name);
            rc = ENOTIMPL;
        }
    }

    if ((m_termios.c_cflag & CSIZE) != CS8) {
        dbgln("FIXME: Character sizes other than 8 bits are not supported");
        rc = ENOTIMPL;
    }

    constexpr FlagDescription unimplemented_cflags[] = {
        { CSTOPB, "CSTOPB"sv },
        { CREAD, "CREAD"sv },
        { PARENB, "PARENB"sv },
        { PARODD, "PARODD"sv },
        { HUPCL, "HUPCL"sv },
        { CLOCAL, "CLOCAL"sv }
    };
    for (auto flag : unimplemented_cflags) {
        if (m_termios.c_cflag & flag.value) {
            dbgln("FIXME: cflag {} unimplemented", flag.name);
            rc = ENOTIMPL;
        }
    }

    constexpr FlagDescription unimplemented_lflags[] = {
        { TOSTOP, "TOSTOP"sv },
        { IEXTEN, "IEXTEN"sv }
    };
    for (auto flag : unimplemented_lflags) {
        if (m_termios.c_lflag & flag.value) {
            dbgln("FIXME: lflag {} unimplemented", flag.name);
            rc = ENOTIMPL;
        }
    }

    // FIXME: decouple VMIN/VTIME support from the description's blocking status
    // FIXME: support VMIN > 1 where we should block until VMIN characters are available
    // FIXME: implement support for VTIME as an interbyte timeout
    description.set_blocking(in_canonical_mode() || m_termios.c_cc[VMIN] > 0);

    return rc;
}

ErrorOr<void> TTY::ioctl(OpenFileDescription& description, unsigned request, Userspace<void*> arg)
{
    auto& current_process = Process::current();
    TRY(current_process.require_promise(Pledge::tty));
#if 0
    // FIXME: When should we block things?
    //        How do we make this work together with MasterPTY forwarding to us?
    if (current_process.tty() && current_process.tty() != this) {
        return ENOTTY;
    }
#endif
    switch (request) {
    case TIOCGPGRP: {
        auto user_pgid = static_ptr_cast<pid_t*>(arg);
        auto pgid = this->pgid().value();
        return copy_to_user(user_pgid, &pgid);
    }
    case TIOCSPGRP: {
        ProcessGroupID pgid = static_cast<pid_t>(arg.ptr());
        if (pgid <= 0)
            return EINVAL;
        InterruptDisabler disabler;
        auto process_group = ProcessGroup::from_pgid(pgid);
        // Disallow setting a nonexistent PGID.
        if (!process_group)
            return EINVAL;

        auto process = Process::from_pid_in_same_process_list(ProcessID(pgid.value()));
        SessionID new_sid = process ? process->sid() : Process::get_sid_from_pgid(pgid);
        if (!new_sid || new_sid != current_process.sid())
            return EPERM;
        if (process && pgid != process->pgid())
            return EPERM;
        m_pg = TRY(process_group->try_make_weak_ptr());

        if (process) {
            if (auto parent = Process::from_pid_ignoring_process_lists(process->ppid())) {
                m_original_process_parent = *parent;
                return {};
            }
        }

        m_original_process_parent = nullptr;
        return {};
    }
    case TCGETS: {
        auto user_termios = static_ptr_cast<termios*>(arg);
        return copy_to_user(user_termios, &m_termios);
    }
    case TCSETS:
    case TCSETSF:
    case TCSETSW: {
        auto user_termios = static_ptr_cast<termios const*>(arg);
        auto termios = TRY(copy_typed_from_user(user_termios));
        auto rc = set_termios(description, termios);
        if (request == TCSETSF)
            flush_input();
        return rc;
    }
    case TCFLSH: {
        // Serenity's TTY implementation does not use an output buffer, so ignore TCOFLUSH.
        auto operation = static_cast<u8>(arg.ptr());
        if (operation == TCIFLUSH || operation == TCIOFLUSH) {
            flush_input();
        } else if (operation != TCOFLUSH) {
            return EINVAL;
        }
        return {};
    }
    case TIOCGWINSZ: {
        auto user_winsize = static_ptr_cast<winsize*>(arg);
        winsize ws {};
        ws.ws_row = m_rows;
        ws.ws_col = m_columns;
        ws.ws_xpixel = 0;
        ws.ws_ypixel = 0;
        return copy_to_user(user_winsize, &ws);
    }
    case TIOCSWINSZ: {
        auto user_winsize = static_ptr_cast<winsize const*>(arg);
        auto ws = TRY(copy_typed_from_user(user_winsize));
        if (ws.ws_col == m_columns && ws.ws_row == m_rows)
            return {};
        m_rows = ws.ws_row;
        m_columns = ws.ws_col;
        generate_signal(SIGWINCH);
        return {};
    }
    case TIOCSCTTY:
        current_process.set_tty(this);
        return {};
    case TIOCSTI:
        return EIO;
    case TIOCNOTTY:
        current_process.set_tty(nullptr);
        return {};
    }
    return EINVAL;
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
