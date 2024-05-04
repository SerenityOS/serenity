/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularDeque.h>
#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Library/DoubleBuffer.h>
#include <Kernel/Library/LockWeakPtr.h>
#include <Kernel/Tasks/ProcessGroup.h>
#include <Kernel/UnixTypes.h>

#define TTY_BUFFER_SIZE 1024

namespace Kernel {

class TTY : public CharacterDevice {
public:
    virtual ~TTY() override;

    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override;
    virtual bool can_read(OpenFileDescription const&, u64) const override;
    virtual bool can_write(OpenFileDescription const&, u64) const override;
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;

    unsigned short rows() const { return m_rows; }
    unsigned short columns() const { return m_columns; }

    ProcessGroupID pgid() const
    {
        if (auto pg = m_pg.strong_ref())
            return pg->pgid();
        return 0;
    }

    bool should_generate_signals() const { return (m_termios.c_lflag & ISIG) == ISIG; }
    bool should_flush_on_signal() const { return (m_termios.c_lflag & NOFLSH) != NOFLSH; }
    bool should_echo_input() const { return (m_termios.c_lflag & ECHO) == ECHO; }
    bool in_canonical_mode() const { return (m_termios.c_lflag & ICANON) == ICANON; }

    void set_default_termios();
    void hang_up();

    virtual ErrorOr<NonnullOwnPtr<KString>> pseudo_name() const = 0;

protected:
    virtual ErrorOr<size_t> on_tty_write(UserOrKernelBuffer const&, size_t) = 0;
    void set_size(unsigned short columns, unsigned short rows);

    TTY(MajorAllocation::CharacterDeviceFamily, MinorNumber minor);
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
    ErrorOr<void> set_termios(OpenFileDescription&, termios const&);

    template<typename Functor>
    void process_output(u8, Functor put_char);

    CircularDeque<u8, TTY_BUFFER_SIZE> m_input_buffer;
    // FIXME: use something like AK::Bitmap but which takes a size template parameter
    u8 m_special_character_bitmask[TTY_BUFFER_SIZE / 8];

    LockWeakPtr<Process> m_original_process_parent;
    LockWeakPtr<ProcessGroup> m_pg;
    termios m_termios;
    unsigned short m_rows { 0 };
    unsigned short m_columns { 0 };
};

}
