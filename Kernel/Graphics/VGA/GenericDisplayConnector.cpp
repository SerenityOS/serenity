/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Graphics/Console/TextModeConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/VGA/GenericDisplayConnector.h>

namespace Kernel {

NonnullOwnPtr<VGAGenericDisplayConnector> VGAGenericDisplayConnector::must_create_with_preset_resolution(PhysicalAddress framebuffer_address, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch)
{
    auto connector = adopt_own_if_nonnull(new (nothrow) VGAGenericDisplayConnector(framebuffer_address, framebuffer_width, framebuffer_height, framebuffer_pitch)).release_nonnull();
    MUST(connector->create_attached_framebuffer_console());
    return connector;
}

NonnullOwnPtr<VGAGenericDisplayConnector> VGAGenericDisplayConnector::must_create()
{
    auto connector = adopt_own_if_nonnull(new (nothrow) VGAGenericDisplayConnector()).release_nonnull();
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

void VGAGenericDisplayConnector::enable_console()
{
    VERIFY(m_framebuffer_console);
    m_framebuffer_console->enable();
}
void VGAGenericDisplayConnector::disable_console()
{
    VERIFY(m_framebuffer_console);
    m_framebuffer_console->disable();
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

ErrorOr<DisplayConnector::Resolution> VGAGenericDisplayConnector::get_resolution()
{
    if ((m_framebuffer_width == 0) || (m_framebuffer_height == 0) || (m_framebuffer_pitch == 0))
        return Error::from_errno(ENOTSUP);
    return Resolution { m_framebuffer_width, m_framebuffer_height, 32, {} };
}

}
