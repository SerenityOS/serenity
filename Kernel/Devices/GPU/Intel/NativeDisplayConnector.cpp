/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Delay.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/GPU/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Devices/GPU/Intel/DisplayConnectorGroup.h>
#include <Kernel/Devices/GPU/Intel/NativeDisplayConnector.h>
#include <Kernel/Devices/GPU/Management.h>
#include <Kernel/Memory/Region.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<IntelNativeDisplayConnector>> IntelNativeDisplayConnector::try_create_with_display_connector_group(IntelDisplayConnectorGroup const& parent_connector_group, ConnectorIndex connector_index, Type type, PhysicalAddress framebuffer_address, size_t framebuffer_resource_size)
{
    return TRY(Device::try_create_device<IntelNativeDisplayConnector>(parent_connector_group, connector_index, type, framebuffer_address, framebuffer_resource_size));
}

ErrorOr<void> IntelNativeDisplayConnector::create_attached_framebuffer_console(Badge<IntelDisplayConnectorGroup>)
{
    size_t width = 0;
    size_t height = 0;
    size_t pitch = 0;
    {
        SpinlockLocker control_locker(m_control_lock);
        SpinlockLocker mode_set_locker(m_modeset_lock);
        width = m_current_mode_setting.horizontal_active;
        height = m_current_mode_setting.vertical_active;
        pitch = m_current_mode_setting.horizontal_stride;
    }
    m_framebuffer_console = Graphics::ContiguousFramebufferConsole::initialize(m_framebuffer_address.value(), width, height, pitch);
    GraphicsManagement::the().set_console(*m_framebuffer_console);
    return {};
}

IntelNativeDisplayConnector::IntelNativeDisplayConnector(IntelDisplayConnectorGroup const& parent_connector_group, ConnectorIndex connector_index, Type type, PhysicalAddress framebuffer_address, size_t framebuffer_resource_size)
    : DisplayConnector(framebuffer_address, framebuffer_resource_size, Memory::MemoryType::NonCacheable)
    , m_type(type)
    , m_connector_index(connector_index)
    , m_parent_connector_group(parent_connector_group)
{
}

void IntelNativeDisplayConnector::set_edid_bytes(Badge<IntelDisplayConnectorGroup>, Array<u8, 128> const& raw_bytes)
{
    // Note: The provided EDID might be invalid (because there's no attached monitor)
    // Therefore, set might_be_invalid to true to indicate that.
    DisplayConnector::set_edid_bytes(raw_bytes, true);
}

ErrorOr<void> IntelNativeDisplayConnector::set_y_offset(size_t)
{
    return Error::from_errno(ENOTIMPL);
}

ErrorOr<void> IntelNativeDisplayConnector::unblank()
{
    return Error::from_errno(ENOTIMPL);
}

ErrorOr<void> IntelNativeDisplayConnector::set_safe_mode_setting()
{
    SpinlockLocker locker(m_modeset_lock);
    return m_parent_connector_group->set_safe_mode_setting({}, *this);
}

void IntelNativeDisplayConnector::enable_console()
{
    VERIFY(m_control_lock.is_locked());
    if (m_framebuffer_console)
        m_framebuffer_console->enable();
}

void IntelNativeDisplayConnector::disable_console()
{
    VERIFY(m_control_lock.is_locked());
    if (m_framebuffer_console)
        m_framebuffer_console->disable();
}

ErrorOr<void> IntelNativeDisplayConnector::flush_first_surface()
{
    return Error::from_errno(ENOTSUP);
}

ErrorOr<void> IntelNativeDisplayConnector::set_mode_setting(DisplayConnector::ModeSetting const&)
{
    return Error::from_errno(ENOTIMPL);
}

}
