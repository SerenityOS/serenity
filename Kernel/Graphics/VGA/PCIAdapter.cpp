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

UNMAP_AFTER_INIT NonnullRefPtr<PCIVGACompatibleAdapter> PCIVGACompatibleAdapter::initialize_with_preset_resolution(PCI::DeviceIdentifier const& pci_device_identifier, PhysicalAddress m_framebuffer_address, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch)
{
    return adopt_ref(*new PCIVGACompatibleAdapter(pci_device_identifier.address(), m_framebuffer_address, framebuffer_width, framebuffer_height, framebuffer_pitch));
}

UNMAP_AFTER_INIT NonnullRefPtr<PCIVGACompatibleAdapter> PCIVGACompatibleAdapter::initialize(PCI::DeviceIdentifier const& pci_device_identifier)
{
    return adopt_ref(*new PCIVGACompatibleAdapter(pci_device_identifier.address()));
}

UNMAP_AFTER_INIT void PCIVGACompatibleAdapter::initialize_framebuffer_devices()
{
    // We might not have any pre-set framebuffer, so if that's the case - don't try to initialize one.
    if (m_framebuffer_address.is_null())
        return;
    VERIFY(m_framebuffer_width);
    VERIFY(m_framebuffer_width != 0);
    VERIFY(m_framebuffer_height != 0);
    VERIFY(m_framebuffer_pitch != 0);
    m_framebuffer_device = FramebufferDevice::create(*this, m_framebuffer_address, m_framebuffer_width, m_framebuffer_height, m_framebuffer_pitch);
    // FIXME: Would be nice to be able to return ErrorOr<void> here.
    VERIFY(!m_framebuffer_device->try_to_initialize().is_error());
}

UNMAP_AFTER_INIT PCIVGACompatibleAdapter::PCIVGACompatibleAdapter(PCI::Address address)
    : PCI::Device(address)
{
    m_framebuffer_console = Graphics::TextModeConsole::initialize();
    GraphicsManagement::the().set_console(*m_framebuffer_console);
}

UNMAP_AFTER_INIT PCIVGACompatibleAdapter::PCIVGACompatibleAdapter(PCI::Address address, PhysicalAddress framebuffer_address, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch)
    : PCI::Device(address)
    , m_framebuffer_address(framebuffer_address)
    , m_framebuffer_width(framebuffer_width)
    , m_framebuffer_height(framebuffer_height)
    , m_framebuffer_pitch(framebuffer_pitch)
{
    m_framebuffer_console = Graphics::ContiguousFramebufferConsole::initialize(framebuffer_address, framebuffer_width, framebuffer_height, framebuffer_pitch);
    GraphicsManagement::the().set_console(*m_framebuffer_console);
}

void PCIVGACompatibleAdapter::enable_consoles()
{
    VERIFY(m_framebuffer_console);
    if (m_framebuffer_device)
        m_framebuffer_device->deactivate_writes();
    m_framebuffer_console->enable();
}
void PCIVGACompatibleAdapter::disable_consoles()
{
    VERIFY(m_framebuffer_device);
    VERIFY(m_framebuffer_console);
    m_framebuffer_console->disable();
    m_framebuffer_device->activate_writes();
}

}
