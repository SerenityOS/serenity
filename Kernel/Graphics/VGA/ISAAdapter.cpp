/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Graphics/Console/TextModeConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/VGA/ISAAdapter.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<ISAVGAAdapter> ISAVGAAdapter::initialize()
{
    return adopt_ref(*new ISAVGAAdapter());
}

UNMAP_AFTER_INIT ISAVGAAdapter::ISAVGAAdapter()
{
    m_framebuffer_console = Graphics::TextModeConsole::initialize();
    GraphicsManagement::the().set_console(*m_framebuffer_console);
}

void ISAVGAAdapter::enable_consoles()
{
    VERIFY(m_framebuffer_console);
    m_framebuffer_console->enable();
}
void ISAVGAAdapter::disable_consoles()
{
    VERIFY(m_framebuffer_console);
    m_framebuffer_console->disable();
}

void ISAVGAAdapter::initialize_framebuffer_devices()
{
}

bool ISAVGAAdapter::try_to_set_resolution(size_t, size_t, size_t)
{
    return false;
}
bool ISAVGAAdapter::set_y_offset(size_t, size_t)
{
    return false;
}

}
