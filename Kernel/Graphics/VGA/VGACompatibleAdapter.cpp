/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Graphics/Console/TextModeConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/VGA/VGACompatibleAdapter.h>
#include <Kernel/Sections.h>

namespace Kernel {

void VGACompatibleAdapter::initialize_display_connector_with_preset_resolution(PhysicalAddress framebuffer_address, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch)
{
    m_generic_display_connector = GenericDisplayConnector::must_create_with_preset_resolution(framebuffer_address, framebuffer_width, framebuffer_height, framebuffer_pitch);
}

}
