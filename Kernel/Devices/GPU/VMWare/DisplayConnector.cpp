/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/GPU/Management.h>
#include <Kernel/Devices/GPU/VMWare/Console.h>
#include <Kernel/Devices/GPU/VMWare/DisplayConnector.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<VMWareDisplayConnector>> VMWareDisplayConnector::create(VMWareGraphicsAdapter const& parent_adapter, PhysicalAddress framebuffer_address, size_t framebuffer_resource_size)
{
    auto connector = TRY(Device::try_create_device<VMWareDisplayConnector>(parent_adapter, framebuffer_address, framebuffer_resource_size));
    TRY(connector->create_attached_framebuffer_console());
    TRY(connector->initialize_edid_for_generic_monitor(Array<u8, 3> { 'V', 'M', 'W' }));
    return connector;
}

ErrorOr<void> VMWareDisplayConnector::create_attached_framebuffer_console()
{
    m_framebuffer_console = VMWareFramebufferConsole::initialize(*this);
    GraphicsManagement::the().set_console(*m_framebuffer_console);
    return {};
}

VMWareDisplayConnector::VMWareDisplayConnector(VMWareGraphicsAdapter const& parent_adapter, PhysicalAddress framebuffer_address, size_t framebuffer_resource_size)
    : DisplayConnector(framebuffer_address, framebuffer_resource_size, Memory::MemoryType::NonCacheable)
    , m_parent_adapter(parent_adapter)
{
}

ErrorOr<void> VMWareDisplayConnector::set_safe_mode_setting()
{
    // We assume safe resolution is 1024x768x32
    DisplayConnector::ModeSetting safe_mode_setting {
        .horizontal_stride = 1024 * sizeof(u32),
        .pixel_clock_in_khz = 0, // Note: There's no pixel clock in paravirtualized hardware
        .horizontal_active = 1024,
        .horizontal_front_porch_pixels = 0, // Note: There's no horizontal_front_porch_pixels in paravirtualized hardware
        .horizontal_sync_time_pixels = 0,   // Note: There's no horizontal_sync_time_pixels in paravirtualized hardware
        .horizontal_blank_pixels = 0,       // Note: There's no horizontal_blank_pixels in paravirtualized hardware
        .vertical_active = 768,
        .vertical_front_porch_lines = 0, // Note: There's no vertical_front_porch_lines in paravirtualized hardware
        .vertical_sync_time_lines = 0,   // Note: There's no vertical_sync_time_lines in paravirtualized hardware
        .vertical_blank_lines = 0,       // Note: There's no vertical_blank_lines in paravirtualized hardware
        .horizontal_offset = 0,
        .vertical_offset = 0,
    };
    return set_mode_setting(safe_mode_setting);
}

ErrorOr<void> VMWareDisplayConnector::unblank()
{
    return Error::from_errno(ENOTIMPL);
}

void VMWareDisplayConnector::enable_console()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_framebuffer_console);
    m_framebuffer_console->enable();
}

void VMWareDisplayConnector::disable_console()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_framebuffer_console);
    m_framebuffer_console->disable();
}

ErrorOr<void> VMWareDisplayConnector::flush_first_surface()
{
    // FIXME: Cache these values but keep them in sync with the parent adapter.
    auto width = m_parent_adapter->primary_screen_width({});
    auto height = m_parent_adapter->primary_screen_height({});
    m_parent_adapter->primary_screen_flush({}, width, height);
    return {};
}

ErrorOr<void> VMWareDisplayConnector::set_y_offset(size_t)
{
    return Error::from_errno(ENOTSUP);
}

ErrorOr<void> VMWareDisplayConnector::flush_rectangle(size_t, FBRect const&)
{
    // FIXME: It costs really nothing to flush the entire screen (at least in QEMU).
    // Try to implement better partial rectangle flush method instead here.
    VERIFY(m_flushing_lock.is_locked());
    // FIXME: Cache these values but keep them in sync with the parent adapter.
    auto width = m_parent_adapter->primary_screen_width({});
    auto height = m_parent_adapter->primary_screen_height({});
    m_parent_adapter->primary_screen_flush({}, width, height);
    return {};
}

ErrorOr<void> VMWareDisplayConnector::set_mode_setting(ModeSetting const& mode_setting)
{
    SpinlockLocker locker(m_modeset_lock);
    VERIFY(m_framebuffer_console);
    size_t width = mode_setting.horizontal_active;
    size_t height = mode_setting.vertical_active;

    if (Checked<size_t>::multiplication_would_overflow(width, height, sizeof(u32)))
        return EOVERFLOW;

    TRY(m_parent_adapter->modeset_primary_screen_resolution({}, width, height));

    m_framebuffer_console->set_resolution(width, height, width * sizeof(u32));

    auto pitch = m_parent_adapter->primary_screen_pitch({});
    DisplayConnector::ModeSetting current_mode_setting {
        .horizontal_stride = pitch,
        .pixel_clock_in_khz = 0, // Note: There's no pixel clock in paravirtualized hardware
        .horizontal_active = width,
        .horizontal_front_porch_pixels = 0, // Note: There's no horizontal_front_porch_pixels in paravirtualized hardware
        .horizontal_sync_time_pixels = 0,   // Note: There's no horizontal_sync_time_pixels in paravirtualized hardware
        .horizontal_blank_pixels = 0,       // Note: There's no horizontal_blank_pixels in paravirtualized hardware
        .vertical_active = height,
        .vertical_front_porch_lines = 0, // Note: There's no vertical_front_porch_lines in paravirtualized hardware
        .vertical_sync_time_lines = 0,   // Note: There's no vertical_sync_time_lines in paravirtualized hardware
        .vertical_blank_lines = 0,       // Note: There's no vertical_blank_lines in paravirtualized hardware
        .horizontal_offset = 0,
        .vertical_offset = 0,
    };
    m_current_mode_setting = current_mode_setting;
    return {};
}

}
