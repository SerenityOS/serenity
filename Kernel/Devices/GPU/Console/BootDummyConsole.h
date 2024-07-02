/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/GPU/Console/GenericFramebufferConsole.h>
#include <Kernel/Forward.h>

namespace Kernel::Graphics {

class BootDummyConsole final : public GenericFramebufferConsoleImpl {
public:
    virtual void clear(size_t, size_t, size_t) override { }
    virtual void write(size_t, size_t, char, Color, Color, bool) override { }
    using GenericFramebufferConsoleImpl::write;

    virtual void enable() override { }
    virtual void disable() override { }

    virtual void flush(size_t, size_t, size_t, size_t) override { }
    virtual void set_resolution(size_t, size_t, size_t) override { VERIFY_NOT_REACHED(); }

    // NOTE: These values are just "dumb" values, because the VirtualConsole
    // will query the size of this console when needed.
    BootDummyConsole()
        : GenericFramebufferConsoleImpl(1024, 768, (1024 * sizeof(u32)))
    {
    }

private:
    virtual void set_cursor(size_t, size_t) override { }
    virtual void hide_cursor() override { }
    virtual void show_cursor() override { }
    virtual void clear_glyph(size_t, size_t) override { }

    virtual u8* framebuffer_data() override { VERIFY_NOT_REACHED(); }
};

}
