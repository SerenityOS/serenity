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

#pragma once

#include <AK/CircularDeque.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/DoubleBuffer.h>
#include <Kernel/UnixTypes.h>

class Process;

class TTY : public CharacterDevice {
public:
    virtual ~TTY() override;

    virtual ssize_t read(FileDescription&, u8*, ssize_t) override;
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override;
    virtual bool can_read(const FileDescription&) const override;
    virtual bool can_write(const FileDescription&) const override;
    virtual int ioctl(FileDescription&, unsigned request, unsigned arg) override final;
    virtual String absolute_path(const FileDescription&) const override { return tty_name(); }

    virtual StringView tty_name() const = 0;

    unsigned short rows() const { return m_rows; }
    unsigned short columns() const { return m_columns; }

    void set_pgid(pid_t pgid) { m_pgid = pgid; }
    pid_t pgid() const { return m_pgid; }

    void set_termios(const termios&);
    bool should_generate_signals() const { return m_termios.c_lflag & ISIG; }
    bool should_flush_on_signal() const { return !(m_termios.c_lflag & NOFLSH); }
    bool should_echo_input() const { return m_termios.c_lflag & ECHO; }
    bool in_canonical_mode() const { return m_termios.c_lflag & ICANON; }

    void set_default_termios();
    void hang_up();

protected:
    virtual ssize_t on_tty_write(const u8*, ssize_t) = 0;
    void set_size(unsigned short columns, unsigned short rows);

    TTY(unsigned major, unsigned minor);
    void emit(u8);
    virtual void echo(u8) = 0;

    bool can_do_backspace() const;
    void do_backspace();
    void erase_word();
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

    CircularDeque<u8, 1024> m_input_buffer;
    pid_t m_pgid { 0 };
    termios m_termios;
    unsigned short m_rows { 0 };
    unsigned short m_columns { 0 };
};
