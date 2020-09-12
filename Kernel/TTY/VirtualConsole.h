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

#include <Kernel/Console.h>
#include <Kernel/Devices/KeyboardDevice.h>
#include <Kernel/TTY/TTY.h>
#include <LibVT/Terminal.h>

namespace Kernel {

static constexpr unsigned s_max_virtual_consoles = 6;

class VirtualConsole final : public TTY
    , public KeyboardClient
    , public VT::TerminalClient {
    AK_MAKE_ETERNAL
public:
    VirtualConsole(const unsigned index);
    virtual ~VirtualConsole() override;

    static void switch_to(unsigned);
    static void initialize();

    bool is_graphical() { return m_graphical; }
    void set_graphical(bool graphical);

private:
    // ^KeyboardClient
    virtual void on_key_pressed(KeyboardDevice::Event) override;

    // ^TTY
    virtual ssize_t on_tty_write(const UserOrKernelBuffer&, ssize_t) override;
    virtual String tty_name() const override { return m_tty_name; }
    virtual void echo(u8) override;

    // ^TerminalClient
    virtual void beep() override;
    virtual void set_window_title(const StringView&) override;
    virtual void set_window_progress(int, int) override;
    virtual void terminal_did_resize(u16 columns, u16 rows) override;
    virtual void terminal_history_changed() override;
    virtual void emit(const u8*, size_t) override;

    // ^CharacterDevice
    virtual const char* class_name() const override { return "VirtualConsole"; }

    void set_active(bool);

    void flush_vga_cursor();
    void flush_dirty_lines();

    unsigned m_index;
    bool m_active { false };
    bool m_graphical { false };

    void clear_vga_row(u16 row);
    void set_vga_start_row(u16 row);
    u16 m_vga_start_row { 0 };
    u16 m_current_vga_start_address { 0 };
    u8* m_current_vga_window { nullptr };

    VT::Terminal m_terminal;

    String m_tty_name;
};

}
