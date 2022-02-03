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

UNMAP_AFTER_INIT void PCIVGAGenericAdapter::initialize_framebuffer_devices()
{
    // We might not have any pre-set framebuffer, so if that's the case - don't try to initialize one.
    auto resolution_or_error = m_display_connector->get_resolution();
    if (resolution_or_error.is_error() || !m_framebuffer_address.has_value())
        return;
    auto resolution = resolution_or_error.release_value();
    VERIFY(resolution.width != 0);
    VERIFY(resolution.height != 0);
    m_framebuffer_device = FramebufferDevice::create(*this, m_framebuffer_address.value(), resolution.width, resolution.height, resolution.width * sizeof(u32));
    // FIXME: Would be nice to be able to return ErrorOr<void> here.
    VERIFY(!m_framebuffer_device->try_to_initialize().is_error());
}

void PCIVGAGenericAdapter::enable_consoles()
{
    if (m_framebuffer_device)
        m_framebuffer_device->deactivate_writes();
    m_display_connector->enable_console();
}
void PCIVGAGenericAdapter::disable_consoles()
{
    VERIFY(m_framebuffer_device);
    m_display_connector->disable_console();
    m_framebuffer_device->activate_writes();
}

ErrorOr<void> PCIVGAGenericAdapter::initialize_adapter_with_preset_resolution(PhysicalAddress framebuffer_address, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch)
{
    dmesgln("PCI VGA Adapter @ {}", pci_address());
    m_framebuffer_address = framebuffer_address;
    m_display_connector = VGAGenericDisplayConnector::must_create_with_preset_resolution(framebuffer_address, framebuffer_width, framebuffer_height, framebuffer_pitch);
    return {};
}

ErrorOr<void> PCIVGAGenericAdapter::initialize_adapter()
{
    dmesgln("PCI VGA Adapter @ {}", pci_address());
    m_display_connector = VGAGenericDisplayConnector::must_create();
    return {};
}

UNMAP_AFTER_INIT PCIVGAGenericAdapter::PCIVGAGenericAdapter(PCI::Address address)
    : PCI::Device(address)
{
}

}
