/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Graphics/Generic/DisplayConnector.h>
#include <Kernel/Graphics/GraphicsManagement.h>

namespace Kernel {

NonnullLockRefPtr<GenericDisplayConnector> GenericDisplayConnector::must_create_with_preset_resolution(PhysicalAddress framebuffer_address, size_t width, size_t height, size_t pitch)
{
    auto device_or_error = DeviceManagement::try_create_device<GenericDisplayConnector>(framebuffer_address, width, height, pitch);
    VERIFY(!device_or_error.is_error());
    auto connector = device_or_error.release_value();
    MUST(connector->create_attached_framebuffer_console());
    MUST(connector->initialize_edid_for_generic_monitor({}));
    return connector;
}

GenericDisplayConnector::GenericDisplayConnector(PhysicalAddress framebuffer_address, size_t width, size_t height, size_t pitch)
    : DisplayConnector(framebuffer_address, height * pitch, true)
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
