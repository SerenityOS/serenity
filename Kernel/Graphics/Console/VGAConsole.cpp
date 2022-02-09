/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/Console/VGAConsole.h>
#include <Kernel/Sections.h>

namespace Kernel::Graphics {

UNMAP_AFTER_INIT VGAConsole::VGAConsole(Mode mode, size_t width, size_t height)
    : Console(width, height)
    , m_vga_region(MM.allocate_kernel_region(PhysicalAddress(0xa0000), Memory::page_round_up(0xc0000 - 0xa0000).release_value_but_fixme_should_propagate_errors(), "VGA Display", Memory::Region::Access::ReadWrite).release_value())
    , m_mode(mode)
{
}

}
