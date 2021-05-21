/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularDeque.h>
#include <AK/WeakPtr.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/DoubleBuffer.h>
#include <Kernel/ProcessGroup.h>
#include <Kernel/UnixTypes.h>
#define TTYDEFCHARS
#include <LibC/sys/ttydefaults.h>
#undef TTYDEFCHARS

namespace Kernel {

class TTY : public CharacterDevice {
public:
    virtual ~TTY() override;

    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override;
    virtual bool can_read(const FileDescription&, size_t) const override;
    virtual bool can_write(const FileDescription&, size_t) const override;
    virtual int ioctl(FileDescription&, unsigned request, FlatPtr arg) override final;
    virtual String absolute_path(const FileDescription&) const override { return tty_name(); }

    virtual String const& tty_name() const = 0;

    unsigned short rows() const { return m_rows; }
    unsigned short columns() const { return m_columns; }

    ProcessGroupID pgid() const
    {
        if (auto pg = m_pg.strong_ref())
            return pg->pgid();
        return 0;
    }

    int set_termios(const termios& new_termios, bool force = false);
    bool should_generate_signals() const { return m_termios.c_lflag & ISIG; }
    bool should_flush_on_signal() const { return !(m_termios.c_lflag & NOFLSH); }
    bool should_echo_input() const { return m_termios.c_lflag & ECHO; }
    bool in_canonical_mode() const { return m_termios.c_lflag & ICANON; }

    const termios& get_termios() const { return m_termios; }
    void hang_up();

    // ^Device
    virtual mode_t required_mode() const override { return 0620; }

    static constexpr termios DEFAULT_TERMIOS = {
        .c_iflag = TTYDEF_IFLAG,
        .c_oflag = TTYDEF_OFLAG,
        .c_cflag = TTYDEF_CFLAG,
        .c_lflag = TTYDEF_LFLAG,
        .c_cc = { TTYDEF_CC },
        .c_ispeed = TTYDEF_SPEED,
        .c_ospeed = TTYDEF_SPEED
    };

protected:
    enum ParityMode {
        None,
        Even,
        Odd
    };

    enum StopBits {
        One,
        Two,
    };

    enum CharacterSize {
        FiveBits,
        SixBits,
        SevenBits,
        EightBits
    };

    virtual ssize_t on_tty_write(const UserOrKernelBuffer&, ssize_t) = 0;
    virtual void echo(u8) = 0;
    virtual int change_baud(speed_t, speed_t) { return 0; }
    virtual int change_parity(ParityMode) { return 0; }
    virtual int change_stop_bits(StopBits) { return 0; }
    virtual int change_character_size(CharacterSize) { return 0; }
    virtual int change_receiver_enabled(bool) { return 0; }
    virtual int change_ignore_modem_status(bool) { return 0; }
    virtual void discard_pending_input() { }
    virtual void discard_pending_output() { }
    virtual void wait_until_pending_output_completes() { }

    void set_size(unsigned short columns, unsigned short rows);

    TTY(unsigned major, unsigned minor, termios initial_termios = DEFAULT_TERMIOS);
    void emit(u8, bool do_evaluate_block_conditions = true);

    bool can_do_backspace() const;
    void do_backspace();
    void erase_word();
    void erase_character();
    void kill_line();
    void flush_input();
    void flush_output();

    void reload_current_termios();

    bool is_eol(u8) const;
    bool is_eof(u8) const;
    bool is_kill(u8) const;
    bool is_erase(u8) const;
    bool is_werase(u8) const;

    void generate_signal(int signal);

    int m_available_lines { 0 };

private:
    // ^CharacterDevice
    virtual bool is_tty() const final override { return true; }

    CircularDeque<u8, 1024> m_input_buffer;
    WeakPtr<Process> m_original_process_parent;
    WeakPtr<ProcessGroup> m_pg;
    termios m_termios;
    unsigned short m_rows { 0 };
    unsigned short m_columns { 0 };
};

}
