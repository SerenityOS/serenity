/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020 Sergey Bugaev <bugaevc@serenityos.org>
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

#include "VirtualConsole.h"
#include <AK/String.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Devices/KeyboardDevice.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/IO.h>
#include <Kernel/StdLib.h>

namespace Kernel {

static u8* s_vga_buffer;
static VirtualConsole* s_consoles[s_max_virtual_consoles];
static int s_active_console;
static RecursiveSpinLock s_lock;

void VirtualConsole::flush_vga_cursor()
{
    u16 value = m_current_vga_start_address + (m_terminal.cursor_row() * columns() + m_terminal.cursor_column());
    IO::out8(0x3d4, 0x0e);
    IO::out8(0x3d5, MSB(value));
    IO::out8(0x3d4, 0x0f);
    IO::out8(0x3d5, LSB(value));
}

void VirtualConsole::initialize()
{
    s_vga_buffer = (u8*)0xc00b8000;
    s_active_console = -1;
}

void VirtualConsole::set_graphical(bool graphical)
{
    if (graphical)
        set_vga_start_row(0);

    m_graphical = graphical;
}

VirtualConsole::VirtualConsole(const unsigned index)
    : TTY(4, index)
    , m_index(index)
    , m_terminal(*this)
{
    ASSERT(index < s_max_virtual_consoles);

    m_tty_name = String::format("/dev/tty%u", m_index);
    m_terminal.set_size(80, 25);

    s_consoles[index] = this;
}

VirtualConsole::~VirtualConsole()
{
    ASSERT_NOT_REACHED();
}

void VirtualConsole::switch_to(unsigned index)
{
    if ((int)index == s_active_console)
        return;
    ASSERT(index < s_max_virtual_consoles);
    ASSERT(s_consoles[index]);

    ScopedSpinLock lock(s_lock);
    if (s_active_console != -1) {
        auto* active_console = s_consoles[s_active_console];
        // We won't know how to switch away from a graphical console until we
        // can set the video mode on our own. Just stop anyone from trying for
        // now.
        if (active_console->is_graphical()) {
            dbg() << "Cannot switch away from graphical console yet :(";
            return;
        }
        active_console->set_active(false);
    }
    dbg() << "VC: Switch to " << index << " (" << s_consoles[index] << ")";
    s_active_console = index;
    s_consoles[s_active_console]->set_active(true);
}

void VirtualConsole::set_active(bool active)
{
    if (active == m_active)
        return;

    ScopedSpinLock lock(s_lock);

    m_active = active;

    if (active) {
        set_vga_start_row(0);
        KeyboardDevice::the().set_client(this);

        m_terminal.m_need_full_flush = true;
        flush_dirty_lines();
    } else {
        KeyboardDevice::the().set_client(nullptr);
    }
}

enum class VGAColor : u8 {
    Black = 0,
    Blue,
    Green,
    Cyan,
    Red,
    Magenta,
    Brown,
    LightGray,
    DarkGray,
    BrightBlue,
    BrightGreen,
    BrightCyan,
    BrightRed,
    BrightMagenta,
    Yellow,
    White,
};

enum class ANSIColor : u8 {
    Black = 0,
    Red,
    Green,
    Brown,
    Blue,
    Magenta,
    Cyan,
    LightGray,
    DarkGray,
    BrightRed,
    BrightGreen,
    Yellow,
    BrightBlue,
    BrightMagenta,
    BrightCyan,
    White,
    __Count,
};

static inline VGAColor ansi_color_to_vga(ANSIColor color)
{
    switch (color) {
    case ANSIColor::Black:
        return VGAColor::Black;
    case ANSIColor::Red:
        return VGAColor::Red;
    case ANSIColor::Brown:
        return VGAColor::Brown;
    case ANSIColor::Blue:
        return VGAColor::Blue;
    case ANSIColor::Magenta:
        return VGAColor::Magenta;
    case ANSIColor::Green:
        return VGAColor::Green;
    case ANSIColor::Cyan:
        return VGAColor::Cyan;
    case ANSIColor::LightGray:
        return VGAColor::LightGray;
    case ANSIColor::DarkGray:
        return VGAColor::DarkGray;
    case ANSIColor::BrightRed:
        return VGAColor::BrightRed;
    case ANSIColor::BrightGreen:
        return VGAColor::BrightGreen;
    case ANSIColor::Yellow:
        return VGAColor::Yellow;
    case ANSIColor::BrightBlue:
        return VGAColor::BrightBlue;
    case ANSIColor::BrightMagenta:
        return VGAColor::BrightMagenta;
    case ANSIColor::BrightCyan:
        return VGAColor::BrightCyan;
    case ANSIColor::White:
        return VGAColor::White;
    default:
        ASSERT_NOT_REACHED();
    }
}

static inline u8 xterm_color_to_vga(u32 color)
{
    for (u8 i = 0; i < (u8)ANSIColor::__Count; i++) {
        if (xterm_colors[i] == color)
            return (u8)ansi_color_to_vga((ANSIColor)i);
    }
    return (u8)VGAColor::LightGray;
}

void VirtualConsole::clear_vga_row(u16 row)
{
    u16* linemem = (u16*)&m_current_vga_window[row * 160];
    for (u16 i = 0; i < columns(); ++i)
        linemem[i] = 0x0720;
}

void VirtualConsole::on_key_pressed(KeyboardDevice::Event event)
{
    // Ignore keyboard in graphical mode.
    if (m_graphical)
        return;

    if (!event.is_press())
        return;

    if (event.key == KeyCode::Key_PageUp && event.flags == Mod_Shift) {
        // TODO: scroll up
        return;
    }
    if (event.key == KeyCode::Key_PageDown && event.flags == Mod_Shift) {
        // TODO: scroll down
        return;
    }

    m_terminal.handle_key_press(event.key, event.code_point, event.flags);
}

ssize_t VirtualConsole::on_tty_write(const UserOrKernelBuffer& data, ssize_t size)
{
    ScopedSpinLock lock(s_lock);
    ssize_t nread = data.read_buffered<512>((size_t)size, [&](const u8* buffer, size_t buffer_bytes) {
        for (size_t i = 0; i < buffer_bytes; ++i)
            m_terminal.on_input(buffer[i]);
        return (ssize_t)buffer_bytes;
    });
    if (m_active)
        flush_dirty_lines();
    return nread;
}

void VirtualConsole::set_vga_start_row(u16 row)
{
    m_vga_start_row = row;
    m_current_vga_start_address = row * columns();
    m_current_vga_window = s_vga_buffer + row * 160;
    IO::out8(0x3d4, 0x0c);
    IO::out8(0x3d5, MSB(m_current_vga_start_address));
    IO::out8(0x3d4, 0x0d);
    IO::out8(0x3d5, LSB(m_current_vga_start_address));
}

static inline u8 attribute_to_vga(const VT::Attribute& attribute)
{
    u8 vga_attr = 0x07;

    if (attribute.flags & VT::Attribute::Bold)
        vga_attr |= 0x08;

    // Background color
    vga_attr &= ~0x70;
    vga_attr |= xterm_color_to_vga(attribute.background_color) << 8;

    // Foreground color
    vga_attr &= ~0x7;
    vga_attr |= xterm_color_to_vga(attribute.foreground_color);

    return vga_attr;
}

void VirtualConsole::flush_dirty_lines()
{
    for (u16 visual_row = 0; visual_row < m_terminal.rows(); ++visual_row) {
        auto& line = m_terminal.visible_line(visual_row);
        if (!line.is_dirty() && !m_terminal.m_need_full_flush)
            continue;
        for (size_t column = 0; column < line.length(); ++column) {
            u32 code_point = line.code_point(column);
            auto attribute = line.attributes()[column];
            u16 vga_index = (visual_row * 160) + (column * 2);
            m_current_vga_window[vga_index] = code_point < 128 ? code_point : '?';
            m_current_vga_window[vga_index + 1] = attribute_to_vga(attribute);
        }
        line.set_dirty(false);
    }
    flush_vga_cursor();
    m_terminal.m_need_full_flush = false;
}

void VirtualConsole::beep()
{
    // TODO
    dbg() << "Beep!1";
}

void VirtualConsole::set_window_title(const StringView&)
{
    // Do nothing.
}

void VirtualConsole::set_window_progress(int, int)
{
    // Do nothing.
}

void VirtualConsole::terminal_did_resize(u16 columns, u16 rows)
{
    ASSERT(columns == 80);
    ASSERT(rows == 25);
    set_size(columns, rows);
}

void VirtualConsole::terminal_history_changed()
{
    // Do nothing, I guess?
}

void VirtualConsole::emit(const u8* data, size_t size)
{
    for (size_t i = 0; i < size; i++)
        TTY::emit(data[i]);
}

void VirtualConsole::echo(u8 ch)
{
    if (should_echo_input()) {
        auto buffer = UserOrKernelBuffer::for_kernel_buffer(&ch);
        on_tty_write(buffer, 1);
    }
}

}
