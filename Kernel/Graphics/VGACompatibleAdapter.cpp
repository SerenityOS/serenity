/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Graphics/Console/TextModeConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/VGACompatibleAdapter.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<VGACompatibleAdapter> VGACompatibleAdapter::initialize_with_preset_resolution(PCI::DeviceIdentifier const& pci_device_identifier, PhysicalAddress m_framebuffer_address, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch)
{
    return adopt_ref(*new VGACompatibleAdapter(pci_device_identifier.address(), m_framebuffer_address, framebuffer_width, framebuffer_height, framebuffer_pitch));
}

UNMAP_AFTER_INIT NonnullRefPtr<VGACompatibleAdapter> VGACompatibleAdapter::initialize(PCI::DeviceIdentifier const& pci_device_identifier)
{
    return adopt_ref(*new VGACompatibleAdapter(pci_device_identifier.address()));
}

UNMAP_AFTER_INIT void VGACompatibleAdapter::initialize_framebuffer_devices()
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

UNMAP_AFTER_INIT VGACompatibleAdapter::VGACompatibleAdapter(PCI::Address address)
    : PCI::Device(address)
{
    m_framebuffer_console = Graphics::TextModeConsole::initialize(*this);
    // FIXME: This is a very wrong way to do this...
    GraphicsManagement::the().m_console = m_framebuffer_console;
}

UNMAP_AFTER_INIT VGACompatibleAdapter::VGACompatibleAdapter(PCI::Address address, PhysicalAddress framebuffer_address, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch)
    : PCI::Device(address)
    , m_framebuffer_address(framebuffer_address)
    , m_framebuffer_width(framebuffer_width)
    , m_framebuffer_height(framebuffer_height)
    , m_framebuffer_pitch(framebuffer_pitch)
{
    m_framebuffer_console = Graphics::ContiguousFramebufferConsole::initialize(framebuffer_address, framebuffer_width, framebuffer_height, framebuffer_pitch);
    // FIXME: This is a very wrong way to do this...
    GraphicsManagement::the().m_console = m_framebuffer_console;
}

void VGACompatibleAdapter::enable_consoles()
{
    VERIFY(m_framebuffer_console);
    if (m_framebuffer_device)
        m_framebuffer_device->deactivate_writes();
    m_framebuffer_console->enable();
}
void VGACompatibleAdapter::disable_consoles()
{
    VERIFY(m_framebuffer_device);
    VERIFY(m_framebuffer_console);
    m_framebuffer_console->disable();
    m_framebuffer_device->activate_writes();
}

bool VGACompatibleAdapter::try_to_set_resolution(size_t, size_t, size_t)
{
    return false;
}
bool VGACompatibleAdapter::set_y_offset(size_t, size_t)
{
    return false;
}

}
