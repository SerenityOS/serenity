/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/GPU/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Devices/GPU/Generic/DisplayConnector.h>
#include <Kernel/Devices/GPU/Management.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<GenericDisplayConnector>> GenericDisplayConnector::create_with_preset_resolution(PhysicalAddress framebuffer_address, size_t width, size_t height, size_t pitch)
{
    auto connector = TRY(Device::try_create_device<GenericDisplayConnector>(framebuffer_address, width, height, pitch));
    TRY(connector->create_attached_framebuffer_console());
    TRY(connector->initialize_edid_for_generic_monitor({}));
    return connector;
}

GenericDisplayConnector::GenericDisplayConnector(PhysicalAddress framebuffer_address, size_t width, size_t height, size_t pitch)
    : DisplayConnector(framebuffer_address, height * pitch, Memory::MemoryType::NonCacheable)
{
    m_current_mode_setting.horizontal_active = width;
    m_current_mode_setting.vertical_active = height;
    m_current_mode_setting.horizontal_stride = pitch;
}

ErrorOr<void> GenericDisplayConnector::create_attached_framebuffer_console()
{
    auto width = m_current_mode_setting.horizontal_active;
    auto height = m_current_mode_setting.vertical_active;
    auto pitch = m_current_mode_setting.horizontal_stride;

    m_framebuffer_console = Graphics::ContiguousFramebufferConsole::initialize(m_framebuffer_address.value(), width, height, pitch);
    GraphicsManagement::the().set_console(*m_framebuffer_console);
    return {};
}

void GenericDisplayConnector::enable_console()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_framebuffer_console);
    m_framebuffer_console->enable();
}

void GenericDisplayConnector::disable_console()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_framebuffer_console);
    m_framebuffer_console->disable();
}

ErrorOr<void> GenericDisplayConnector::flush_first_surface()
{
    return Error::from_errno(ENOTSUP);
}

}
