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

#include <Kernel/Devices/KeyboardDevice.h>
#include <Kernel/TTY/TTY.h>
#include <LibBareMetal/Output/Console.h>

namespace Kernel {

class VirtualConsole final : public TTY
    , public KeyboardClient
    , public ConsoleImplementation {
    AK_MAKE_ETERNAL
public:
    enum InitialContents {
        Cleared,
        AdoptCurrentVGABuffer
    };

    VirtualConsole(unsigned index, InitialContents = Cleared);
    virtual ~VirtualConsole() override;

    static void switch_to(unsigned);
    static void initialize();

    bool is_graphical() { return m_graphical; }
    void set_graphical(bool graphical);

private:
    // ^KeyboardClient
    virtual void on_key_pressed(KeyboardDevice::Event) override;

    // ^ConsoleImplementation
    virtual void on_sysconsole_receive(u8) override;

    // ^TTY
    virtual ssize_t on_tty_write(const u8*, ssize_t) override;
    virtual StringView tty_name() const override;
    virtual void echo(u8) override;

    // ^CharacterDevice
    virtual const char* class_name() const override { return "VirtualConsole"; }

    void set_active(bool);
    void on_char(u8);

    void get_vga_cursor(u8& row, u8& column);
    void flush_vga_cursor();

    u8* m_buffer;
    unsigned m_index;
    bool m_active { false };
    bool m_graphical { false };

    void scroll_up();
    void set_cursor(unsigned row, unsigned column);
    void put_character_at(unsigned row, unsigned column, u8 ch);

    void escape$A(const Vector<unsigned>&);
    void escape$D(const Vector<unsigned>&);
    void escape$H(const Vector<unsigned>&);
    void escape$J(const Vector<unsigned>&);
    void escape$m(const Vector<unsigned>&);
    void escape$s(const Vector<unsigned>&);
    void escape$u(const Vector<unsigned>&);

    void clear();

    u8 m_cursor_row { 0 };
    u8 m_cursor_column { 0 };
    u8 m_saved_cursor_row { 0 };
    u8 m_saved_cursor_column { 0 };
    u8 m_current_attribute { 0x07 };

    void clear_vga_row(u16 row);
    void set_vga_start_row(u16 row);
    u16 m_vga_start_row { 0 };
    u16 m_current_vga_start_address { 0 };
    u8* m_current_vga_window { nullptr };

    void execute_escape_sequence(u8 final);

    enum EscapeState {
        Normal,
        ExpectBracket,
        ExpectParameter,
        ExpectIntermediate,
        ExpectFinal,
    };
    EscapeState m_escape_state { Normal };
    Vector<u8> m_parameters;
    Vector<u8> m_intermediates;
    u8* m_horizontal_tabs { nullptr };
    char m_tty_name[32];
};

}
