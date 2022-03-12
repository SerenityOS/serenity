/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Graphics/Console/TextModeConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/VGA/GenericDisplayConnector.h>

namespace Kernel {

NonnullRefPtr<VGAGenericDisplayConnector> VGAGenericDisplayConnector::must_create_with_preset_resolution(PhysicalAddress framebuffer_address, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch)
{
    auto device_or_error = DeviceManagement::try_create_device<VGAGenericDisplayConnector>(framebuffer_address, framebuffer_width, framebuffer_height, framebuffer_pitch);
    VERIFY(!device_or_error.is_error());
    auto connector = device_or_error.release_value();
    MUST(connector->create_attached_framebuffer_console());
    return connector;
}

NonnullRefPtr<VGAGenericDisplayConnector> VGAGenericDisplayConnector::must_create()
{
    auto connector = adopt_ref_if_nonnull(new (nothrow) VGAGenericDisplayConnector()).release_nonnull();
    MUST(connector->create_attached_text_console());
    return connector;
}

ErrorOr<void> VGAGenericDisplayConnector::create_attached_text_console()
{
    m_framebuffer_console = Graphics::TextModeConsole::initialize();
    GraphicsManagement::the().set_console(*m_framebuffer_console);
    return {};
}

ErrorOr<void> VGAGenericDisplayConnector::create_attached_framebuffer_console()
{
    VERIFY(m_framebuffer_address.has_value());
    m_framebuffer_console = Graphics::ContiguousFramebufferConsole::initialize(m_framebuffer_address.value(), m_framebuffer_width, m_framebuffer_height, m_framebuffer_pitch);
    GraphicsManagement::the().set_console(*m_framebuffer_console);
    return {};
}

VGAGenericDisplayConnector::VGAGenericDisplayConnector(PhysicalAddress framebuffer_address, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch)
    : DisplayConnector()
    , m_framebuffer_address(framebuffer_address)
    , m_framebuffer_width(framebuffer_width)
    , m_framebuffer_height(framebuffer_height)
    , m_framebuffer_pitch(framebuffer_pitch)
{
}

VGAGenericDisplayConnector::VGAGenericDisplayConnector()
    : DisplayConnector()
    , m_framebuffer_address({})
{
}

VGAGenericDisplayConnector::VGAGenericDisplayConnector(PhysicalAddress framebuffer_address)
    : DisplayConnector()
    , m_framebuffer_address(framebuffer_address)
{
}

void VGAGenericDisplayConnector::enable_console()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_framebuffer_console);
    m_framebuffer_console->enable();
}
void VGAGenericDisplayConnector::disable_console()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_framebuffer_console);
    m_framebuffer_console->disable();
}

ErrorOr<size_t> VGAGenericDisplayConnector::write_to_first_surface(u64 offset, UserOrKernelBuffer const& buffer, size_t length)
{
    VERIFY(m_control_lock.is_locked());
    if (offset + length > m_framebuffer_region->size())
        return Error::from_errno(EOVERFLOW);
    TRY(buffer.read(m_framebuffer_region->vaddr().offset(offset).as_ptr(), 0, length));
    return length;
}

ErrorOr<void> VGAGenericDisplayConnector::flush_first_surface()
{
    return Error::from_errno(ENOTSUP);
}

ErrorOr<DisplayConnector::Resolution> VGAGenericDisplayConnector::get_resolution()
{
    if ((m_framebuffer_width == 0) || (m_framebuffer_height == 0) || (m_framebuffer_pitch == 0))
        return Error::from_errno(ENOTSUP);
    return Resolution { m_framebuffer_width, m_framebuffer_height, 32, m_framebuffer_pitch, {} };
}

}
