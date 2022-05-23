/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/VMWare/Console.h>
#include <Kernel/Graphics/VMWare/DisplayConnector.h>

namespace Kernel {

NonnullRefPtr<VMWareDisplayConnector> VMWareDisplayConnector::must_create(VMWareGraphicsAdapter const& parent_adapter, PhysicalAddress framebuffer_address)
{
    auto connector = MUST(DeviceManagement::try_create_device<VMWareDisplayConnector>(parent_adapter, framebuffer_address));
    MUST(connector->create_attached_framebuffer_console());
    MUST(connector->initialize_edid_for_generic_monitor());
    return connector;
}

ErrorOr<void> VMWareDisplayConnector::create_attached_framebuffer_console()
{
    auto rounded_size = TRY(Memory::page_round_up(1024 * sizeof(u32) * 768));
    m_framebuffer_region = TRY(MM.allocate_kernel_region(m_framebuffer_address.page_base(), rounded_size, "Framebuffer"sv, Memory::Region::Access::ReadWrite));
    [[maybe_unused]] auto result = m_framebuffer_region->set_write_combine(true);
    m_framebuffer_data = m_framebuffer_region->vaddr().offset(m_framebuffer_address.offset_in_page()).as_ptr();
    m_framebuffer_console = VMWareFramebufferConsole::initialize(*this);
    GraphicsManagement::the().set_console(*m_framebuffer_console);
    return {};
}

VMWareDisplayConnector::VMWareDisplayConnector(VMWareGraphicsAdapter const& parent_adapter, PhysicalAddress framebuffer_address)
    : DisplayConnector()
    , m_framebuffer_address(framebuffer_address)
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

ErrorOr<size_t> VMWareDisplayConnector::write_to_first_surface(u64 offset, UserOrKernelBuffer const& buffer, size_t length)
{
    VERIFY(m_control_lock.is_locked());
    if (offset + length > m_framebuffer_region->size())
        return Error::from_errno(EOVERFLOW);
    TRY(buffer.read(m_framebuffer_data + offset, 0, length));
    return length;
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

    auto rounded_size = TRY(Memory::page_round_up(width * sizeof(u32) * height));
    m_framebuffer_region = TRY(MM.allocate_kernel_region(m_framebuffer_address.page_base(), rounded_size, "Framebuffer"sv, Memory::Region::Access::ReadWrite));
    [[maybe_unused]] auto result = m_framebuffer_region->set_write_combine(true);
    m_framebuffer_data = m_framebuffer_region->vaddr().offset(m_framebuffer_address.offset_in_page()).as_ptr();
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
