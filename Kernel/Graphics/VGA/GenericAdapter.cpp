/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/VGA/GenericAdapter.h>

namespace Kernel {

ErrorOr<void> VGAGenericAdapter::initialize_adapter_with_preset_resolution(PhysicalAddress framebuffer_address, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch)
{
    m_display_connector = VGAGenericDisplayConnector::must_create_with_preset_resolution(framebuffer_address, framebuffer_width, framebuffer_height, framebuffer_pitch);
    return {};
}

ErrorOr<void> VGAGenericAdapter::initialize_adapter()
{
    m_display_connector = VGAGenericDisplayConnector::must_create();
    return {};
}

}
