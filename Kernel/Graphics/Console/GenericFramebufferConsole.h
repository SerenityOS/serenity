/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Types.h>
#include <Kernel/Graphics/Console/Console.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel::Graphics {

class GenericFramebufferConsoleImpl : public Console {
public:
    virtual size_t bytes_per_base_glyph() const override;
    virtual size_t chars_per_line() const override;

    virtual size_t max_column() const override { return m_width / 8; }
    virtual size_t max_row() const override { return m_height / 8; }

    virtual bool is_hardware_paged_capable() const override { return false; }
    virtual bool has_hardware_cursor() const override { return false; }

    virtual void set_cursor(size_t x, size_t y) override;
    virtual void hide_cursor() override;
    virtual void show_cursor() override;

    virtual void clear(size_t x, size_t y, size_t length) override;
    virtual void write(size_t x, size_t y, char ch, Color background, Color foreground, bool critical = false) override;
    virtual void write(size_t x, size_t y, char ch, bool critical = false) override;
    virtual void write(char ch, bool critical = false) override;

    virtual void enable() override;
    virtual void disable() override;

    virtual void set_resolution(size_t width, size_t height, size_t pitch) = 0;

protected:
    GenericFramebufferConsoleImpl(size_t width, size_t height, size_t pitch)
        : Console(width, height)
        , m_pitch(pitch)
    {
    }
    virtual u8* framebuffer_data() = 0;
    virtual void clear_glyph(size_t x, size_t y);
    size_t m_pitch;
};

class GenericFramebufferConsole : public GenericFramebufferConsoleImpl {
public:
    virtual void clear(size_t x, size_t y, size_t length) override;
    virtual void write(size_t x, size_t y, char ch, Color background, Color foreground, bool critical = false) override;

    virtual void enable() override;
    virtual void disable() override;

protected:
    GenericFramebufferConsole(size_t width, size_t height, size_t pitch)
        : GenericFramebufferConsoleImpl(width, height, pitch)
    {
    }

    virtual void clear_glyph(size_t x, size_t y) override;

    mutable Spinlock m_lock;
};

}
