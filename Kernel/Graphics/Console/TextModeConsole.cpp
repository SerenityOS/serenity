/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Graphics/Console/TextModeConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Sections.h>

namespace Kernel::Graphics {

UNMAP_AFTER_INIT NonnullRefPtr<TextModeConsole> TextModeConsole::initialize()
{
    return adopt_ref(*new TextModeConsole());
}

UNMAP_AFTER_INIT TextModeConsole::TextModeConsole()
    : VGAConsole(VGAConsole::Mode::TextMode, 80, 25)
    , m_current_vga_window(m_vga_region->vaddr().offset(0x18000).as_ptr())
{
    for (size_t index = 0; index < height(); index++) {
        clear_vga_row(index);
    }
    dbgln("Text mode console initialized!");
}

enum VGAColor : u8 {
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

[[maybe_unused]] static inline VGAColor convert_standard_color_to_vga_color(Console::Color color)
{
    switch (color) {
    case Console::Color::Black:
        return VGAColor::Black;
    case Console::Color::Red:
        return VGAColor::Red;
    case Console::Color::Brown:
        return VGAColor::Brown;
    case Console::Color::Blue:
        return VGAColor::Blue;
    case Console::Color::Magenta:
        return VGAColor::Magenta;
    case Console::Color::Green:
        return VGAColor::Green;
    case Console::Color::Cyan:
        return VGAColor::Cyan;
    case Console::Color::LightGray:
        return VGAColor::LightGray;
    case Console::Color::DarkGray:
        return VGAColor::DarkGray;
    case Console::Color::BrightRed:
        return VGAColor::BrightRed;
    case Console::Color::BrightGreen:
        return VGAColor::BrightGreen;
    case Console::Color::Yellow:
        return VGAColor::Yellow;
    case Console::Color::BrightBlue:
        return VGAColor::BrightBlue;
    case Console::Color::BrightMagenta:
        return VGAColor::BrightMagenta;
    case Console::Color::BrightCyan:
        return VGAColor::BrightCyan;
    case Console::Color::White:
        return VGAColor::White;
    default:
        VERIFY_NOT_REACHED();
    }
}

void TextModeConsole::set_cursor(size_t x, size_t y)
{
    SpinlockLocker lock(m_vga_lock);
    GraphicsManagement::the().set_vga_text_mode_cursor(width(), x, y);
    m_x = x;
    m_y = y;
}
void TextModeConsole::hide_cursor()
{
    SpinlockLocker lock(m_vga_lock);
    GraphicsManagement::the().disable_vga_text_mode_console_cursor();
}
void TextModeConsole::show_cursor()
{
    set_cursor(m_x, m_y);
}

void TextModeConsole::clear(size_t x, size_t y, size_t length)
{
    SpinlockLocker lock(m_vga_lock);
    auto* buf = (u16*)m_current_vga_window.offset((x * 2) + (y * width() * 2)).as_ptr();
    for (size_t index = 0; index < length; index++) {
        buf[index] = 0x0720;
    }
}
void TextModeConsole::write(size_t x, size_t y, char ch, bool critical)
{
    write(x, y, ch, m_default_background_color, m_default_foreground_color, critical);
}

void TextModeConsole::write(size_t x, size_t y, char ch, Color background, Color foreground, bool critical)
{
    SpinlockLocker lock(m_vga_lock);
    // If we are in critical printing mode, we need to handle new lines here
    // because there's no other responsible object to do that in the print call path
    if (critical && (ch == '\r' || ch == '\n')) {
        // Disable hardware VGA cursor
        GraphicsManagement::the().disable_vga_text_mode_console_cursor();

        m_x = 0;
        m_y += 1;
        if (m_y >= max_row())
            m_y = 0;
        return;
    }

    auto* buf = (u16*)m_current_vga_window.offset((x * 2) + (y * width() * 2)).as_ptr();
    *buf = foreground << 8 | background << 12 | ch;
    m_x = x + 1;

    if (m_x >= max_column()) {
        m_x = 0;
        m_y = y + 1;
        if (m_y >= max_row())
            m_y = 0;
    }
}

void TextModeConsole::clear_vga_row(u16 row)
{
    clear(0, row, width());
}

void TextModeConsole::write(char ch, bool critical)
{
    write(m_x, m_y, ch, critical);
}

}
