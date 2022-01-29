/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Forward.h>
#include <Kernel/Graphics/Console/GenericFramebufferConsole.h>

namespace Kernel::Graphics {

class BootFramebufferConsole : public GenericFramebufferConsoleImpl {
public:
    virtual void clear(size_t x, size_t y, size_t length) override;
    virtual void write(size_t x, size_t y, char ch, Color background, Color foreground, bool critical = false) override;
    using GenericFramebufferConsoleImpl::write;

    virtual void enable() override;
    virtual void disable() override;

    virtual void flush(size_t, size_t, size_t, size_t) override { }
    virtual void set_resolution(size_t, size_t, size_t) override { }

    BootFramebufferConsole(PhysicalAddress framebuffer_addr, size_t width, size_t height, size_t pitch);

protected:
    virtual void clear_glyph(size_t x, size_t y) override;

    virtual u8* framebuffer_data() override;

    OwnPtr<Memory::Region> m_framebuffer;
    u8* m_framebuffer_data {};
    mutable Spinlock m_lock;
};

}
