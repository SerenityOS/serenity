/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Graphics/Console/TextModeConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/VGA/PCIGenericAdapter.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<PCIVGAGenericAdapter> PCIVGAGenericAdapter::must_create_with_preset_resolution(PCI::DeviceIdentifier const& pci_device_identifier, PhysicalAddress m_framebuffer_address, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch)
{
    auto adapter = adopt_ref_if_nonnull(new (nothrow) PCIVGAGenericAdapter(pci_device_identifier.address())).release_nonnull();
    MUST(adapter->initialize_adapter_with_preset_resolution(m_framebuffer_address, framebuffer_width, framebuffer_height, framebuffer_pitch));
    return adapter;
}

UNMAP_AFTER_INIT NonnullRefPtr<PCIVGAGenericAdapter> PCIVGAGenericAdapter::must_create(PCI::DeviceIdentifier const& pci_device_identifier)
{
    auto adapter = adopt_ref_if_nonnull(new (nothrow) PCIVGAGenericAdapter(pci_device_identifier.address())).release_nonnull();
    MUST(adapter->initialize_adapter());
    return adapter;
}

UNMAP_AFTER_INIT PCIVGAGenericAdapter::PCIVGAGenericAdapter(PCI::Address address)
    : PCI::Device(address)
{
}

}
