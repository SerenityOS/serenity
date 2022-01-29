/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/Console/BootFramebufferConsole.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/MemoryManager.h>

namespace Kernel::Graphics {

BootFramebufferConsole::BootFramebufferConsole(PhysicalAddress framebuffer_addr, size_t width, size_t height, size_t pitch)
    : GenericFramebufferConsoleImpl(width, height, pitch)
{
    // NOTE: We're very early in the boot process, memory allocations shouldn't really fail
    auto framebuffer_end = Memory::page_round_up(framebuffer_addr.offset(height * pitch * sizeof(u32)).get()).release_value();
    m_framebuffer = MM.allocate_kernel_region(framebuffer_addr.page_base(), framebuffer_end - framebuffer_addr.page_base().get(), "Boot Framebuffer"sv, Memory::Region::Access::ReadWrite).release_value();
    [[maybe_unused]] auto result = m_framebuffer->set_write_combine(true);
    m_framebuffer_data = m_framebuffer->vaddr().offset(framebuffer_addr.offset_in_page()).as_ptr();
    memset(m_framebuffer_data, 0, height * pitch * sizeof(u32));
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

u8* BootFramebufferConsole::framebuffer_data()
{
    VERIFY(m_lock.is_locked());
    VERIFY(m_framebuffer_data);
    return m_framebuffer_data;
}

}
