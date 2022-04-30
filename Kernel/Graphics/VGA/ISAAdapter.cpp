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

}
