/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Types.h>
#include <Kernel/Devices/GPU/GPUDevice.h>

namespace Kernel::Graphics {

class Console : public AtomicRefCounted<Console> {
public:
    // Stanadard VGA text mode colors
    enum Color : u8 {
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

public:
    size_t width() const { return m_width; }
    size_t height() const { return m_height; }
    size_t pitch() const { return bytes_per_base_glyph() * width(); }
    virtual size_t max_column() const { return m_width; }
    virtual size_t max_row() const { return m_height; }
    virtual size_t bytes_per_base_glyph() const = 0;
    virtual size_t chars_per_line() const = 0;

    virtual void enable() = 0;
    virtual void disable() = 0;

    virtual bool is_hardware_paged_capable() const = 0;
    virtual bool has_hardware_cursor() const = 0;

    virtual void set_cursor(size_t x, size_t y) = 0;

    virtual void clear(size_t x, size_t y, size_t length) = 0;
    virtual void write(size_t x, size_t y, char ch, Color background, Color foreground, bool critical = false) = 0;
    virtual void write(size_t x, size_t y, char ch, bool critical = false) = 0;
    virtual void write(char ch, bool critical = false) = 0;
    virtual void flush(size_t x, size_t y, size_t width, size_t height) = 0;

    virtual ~Console() = default;

protected:
    virtual void hide_cursor() = 0;
    virtual void show_cursor() = 0;

    virtual void scroll_up() = 0;

    Console(size_t width, size_t height)
        : m_width(width)
        , m_height(height)
    {
        m_enabled.store(true);
    }

    Atomic<bool> m_enabled;
    Color m_default_foreground_color { Color::White };
    Color m_default_background_color { Color::Black };
    size_t m_width;
    size_t m_height;
    mutable size_t m_x { 0 };
    mutable size_t m_y { 0 };
};
}
