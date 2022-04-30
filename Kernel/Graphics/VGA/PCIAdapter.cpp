/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Graphics/Console/TextModeConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/VGA/PCIAdapter.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<PCIVGACompatibleAdapter> PCIVGACompatibleAdapter::initialize_with_preset_resolution(PCI::DeviceIdentifier const& pci_device_identifier, PhysicalAddress framebuffer_address, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch)
{
    auto adapter = adopt_ref(*new PCIVGACompatibleAdapter(pci_device_identifier.address()));
    adapter->initialize_display_connector_with_preset_resolution(framebuffer_address, framebuffer_width, framebuffer_height, framebuffer_pitch);
    return adapter;
}

UNMAP_AFTER_INIT NonnullRefPtr<PCIVGACompatibleAdapter> PCIVGACompatibleAdapter::initialize(PCI::DeviceIdentifier const& pci_device_identifier)
{
    auto adapter = adopt_ref(*new PCIVGACompatibleAdapter(pci_device_identifier.address()));
    return adapter;
}

UNMAP_AFTER_INIT PCIVGACompatibleAdapter::PCIVGACompatibleAdapter(PCI::Address address)
    : PCI::Device(address)
{
}

}
