/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/TTY/ConsoleManagement.h>

namespace Kernel::Graphics {

NonnullRefPtr<ContiguousFramebufferConsole> ContiguousFramebufferConsole::initialize(PhysicalAddress framebuffer_address, size_t width, size_t height, size_t pitch)
{
    return adopt_ref(*new ContiguousFramebufferConsole(framebuffer_address, width, height, pitch));
}

ContiguousFramebufferConsole::ContiguousFramebufferConsole(PhysicalAddress framebuffer_address, size_t width, size_t height, size_t pitch)
    : GenericFramebufferConsole(width, height, pitch)
    , m_framebuffer_address(framebuffer_address)
{
    set_resolution(width, height, pitch);
}

void ContiguousFramebufferConsole::set_resolution(size_t width, size_t height, size_t pitch)
{
    m_width = width;
    m_height = height;
    m_pitch = pitch;

    dbgln("Framebuffer Console: taking {} bytes", Memory::page_round_up(pitch * height));
    auto region_or_error = MM.allocate_kernel_region(m_framebuffer_address, Memory::page_round_up(pitch * height), "Framebuffer Console", Memory::Region::Access::ReadWrite, Memory::Region::Cacheable::Yes);
    VERIFY(!region_or_error.is_error());
    m_framebuffer_region = region_or_error.release_value();

    // Just to start cleanly, we clean the entire framebuffer
    memset(m_framebuffer_region->vaddr().as_ptr(), 0, pitch * height);

    ConsoleManagement::the().resolution_was_changed();
}

}
