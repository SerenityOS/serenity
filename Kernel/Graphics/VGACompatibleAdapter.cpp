/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/VGACompatibleAdapter.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<VGACompatibleAdapter> VGACompatibleAdapter::initialize_with_preset_resolution(PCI::Address address, PhysicalAddress m_framebuffer_address, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch)
{
    return adopt_ref(*new VGACompatibleAdapter(address, m_framebuffer_address, framebuffer_width, framebuffer_height, framebuffer_pitch));
}

UNMAP_AFTER_INIT void VGACompatibleAdapter::initialize_framebuffer_devices()
{
}

UNMAP_AFTER_INIT VGACompatibleAdapter::VGACompatibleAdapter(PCI::Address address)
    : PCI::DeviceController(address)
{
}

UNMAP_AFTER_INIT VGACompatibleAdapter::VGACompatibleAdapter(PCI::Address address, PhysicalAddress framebuffer_address, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch)
    : PCI::DeviceController(address)
{
    m_framebuffer = RawFramebufferDevice::create(*this, framebuffer_address, framebuffer_width, framebuffer_height, framebuffer_pitch);
}

}
