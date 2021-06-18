/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#define TTY_BUFFER_SIZE 1024

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

    int set_termios(const termios&);
    bool should_generate_signals() const { return m_termios.c_lflag & ISIG; }
    bool should_flush_on_signal() const { return !(m_termios.c_lflag & NOFLSH); }
    bool should_echo_input() const { return m_termios.c_lflag & ECHO; }
    bool in_canonical_mode() const { return m_termios.c_lflag & ICANON; }

    void set_default_termios();
    void hang_up();

    // ^Device
    virtual mode_t required_mode() const override { return 0620; }

protected:
    virtual KResultOr<size_t> on_tty_write(const UserOrKernelBuffer&, size_t) = 0;
    void set_size(unsigned short columns, unsigned short rows);

    TTY(unsigned major, unsigned minor);
    void emit(u8, bool do_evaluate_block_conditions = false);
    void echo_with_processing(u8);

    bool can_do_backspace() const;
    void do_backspace();
    void erase_word();
    void erase_character();
    void kill_line();
    void flush_input();

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

    virtual void echo(u8) = 0;

    template<typename Functor>
    void process_output(u8, Functor put_char);

    CircularDeque<u8, TTY_BUFFER_SIZE> m_input_buffer;
    // FIXME: use something like AK::Bitmap but which takes a size template parameter
    u8 m_special_character_bitmask[TTY_BUFFER_SIZE / 8];

    WeakPtr<Process> m_original_process_parent;
    WeakPtr<ProcessGroup> m_pg;
    termios m_termios;
    unsigned short m_rows { 0 };
    unsigned short m_columns { 0 };
};

}
