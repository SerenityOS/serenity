/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Console.h>
#include <Kernel/Devices/HID/HIDManagement.h>
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
    virtual void on_key_pressed(KeyEvent) override;

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

    // ^Device
    virtual String device_name() const override;

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
