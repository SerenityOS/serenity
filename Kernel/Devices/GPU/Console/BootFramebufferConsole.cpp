/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/GPU/Console/BootFramebufferConsole.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/MemoryManager.h>

namespace Kernel::Graphics {

BootFramebufferConsole::BootFramebufferConsole(PhysicalAddress framebuffer_addr, size_t width, size_t height, size_t pitch)
    : GenericFramebufferConsoleImpl(width, height, pitch)
{
    // NOTE: We're very early in the boot process, memory allocations shouldn't really fail
    auto framebuffer_end = Memory::page_round_up(framebuffer_addr.offset(height * pitch).get()).release_value();
    m_framebuffer = MM.allocate_mmio_kernel_region(framebuffer_addr.page_base(), framebuffer_end - framebuffer_addr.page_base().get(), "Boot Framebuffer"sv, Memory::Region::Access::ReadWrite, Memory::MemoryType::NonCacheable).release_value();

    m_framebuffer_data = m_framebuffer->vaddr().offset(framebuffer_addr.offset_in_page()).as_ptr();
    memset(m_framebuffer_data, 0, height * pitch);
}

void BootFramebufferConsole::clear(size_t x, size_t y, size_t length)
{
    SpinlockLocker lock(m_lock);
    if (m_framebuffer_data)
        GenericFramebufferConsoleImpl::clear(x, y, length);
}

void BootFramebufferConsole::clear_glyph(size_t x, size_t y)
{

    VERIFY(m_lock.is_locked());
    GenericFramebufferConsoleImpl::clear_glyph(x, y);
}

void BootFramebufferConsole::enable()
{
    // Once disabled, ignore requests to re-enable
}

void BootFramebufferConsole::disable()
{
    SpinlockLocker lock(m_lock);
    GenericFramebufferConsoleImpl::disable();
    m_framebuffer = nullptr;
    m_framebuffer_data = nullptr;
}

void BootFramebufferConsole::write(size_t x, size_t y, char ch, Color background, Color foreground, bool critical)
{
    SpinlockLocker lock(m_lock);
    if (m_framebuffer_data)
        GenericFramebufferConsoleImpl::write(x, y, ch, background, foreground, critical);
}

void BootFramebufferConsole::set_cursor(size_t x, size_t y)
{
    // Note: To ensure we don't trigger a deadlock, let's assert in
    // case we already locked the spinlock, so we know there's a bug
    // in the call path.
    VERIFY(!m_lock.is_locked());
    SpinlockLocker lock(m_lock);
    hide_cursor();
    m_x = x;
    m_y = y;
    show_cursor();
}

void BootFramebufferConsole::hide_cursor()
{
    VERIFY(m_lock.is_locked());
    GenericFramebufferConsoleImpl::hide_cursor();
}

void BootFramebufferConsole::show_cursor()
{
    VERIFY(m_lock.is_locked());
    GenericFramebufferConsoleImpl::show_cursor();
}

u8* BootFramebufferConsole::framebuffer_data()
{
    VERIFY(m_lock.is_locked());
    VERIFY(m_framebuffer_data);
    return m_framebuffer_data;
}

}
