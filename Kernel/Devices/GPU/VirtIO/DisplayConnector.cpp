/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/VirGL.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/GPU/Management.h>
#include <Kernel/Devices/GPU/VirtIO/Console.h>
#include <Kernel/Devices/GPU/VirtIO/DisplayConnector.h>
#include <Kernel/Devices/GPU/VirtIO/GraphicsAdapter.h>
#include <Kernel/Devices/GPU/VirtIO/Protocol.h>
#include <Kernel/Security/Random.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<VirtIODisplayConnector>> VirtIODisplayConnector::create(VirtIOGraphicsAdapter& graphics_adapter, Graphics::VirtIOGPU::ScanoutID scanout_id)
{
    return TRY(Device::try_create_device<VirtIODisplayConnector>(graphics_adapter, scanout_id));
}

static_assert((MAX_VIRTIOGPU_RESOLUTION_WIDTH * MAX_VIRTIOGPU_RESOLUTION_HEIGHT * sizeof(u32) * 2) % PAGE_SIZE == 0);

VirtIODisplayConnector::VirtIODisplayConnector(VirtIOGraphicsAdapter& graphics_adapter, Graphics::VirtIOGPU::ScanoutID scanout_id)
    : DisplayConnector((MAX_VIRTIOGPU_RESOLUTION_WIDTH * MAX_VIRTIOGPU_RESOLUTION_HEIGHT * sizeof(u32) * 2), Memory::MemoryType::NonCacheable)
    , m_graphics_adapter(graphics_adapter)
    , m_scanout_id(scanout_id)
{
}

void VirtIODisplayConnector::initialize_console(Badge<VirtIOGraphicsAdapter>)
{
    m_console = Kernel::Graphics::VirtIOGPU::Console::initialize(*this);
    GraphicsManagement::the().set_console(*m_console);
}

void VirtIODisplayConnector::set_safe_mode_setting_after_initialization(Badge<VirtIOGraphicsAdapter>)
{
    MUST(set_safe_mode_setting());
}

ErrorOr<void> VirtIODisplayConnector::set_mode_setting(ModeSetting const& mode_setting)
{
    SpinlockLocker locker(m_modeset_lock);
    if (mode_setting.horizontal_active > MAX_VIRTIOGPU_RESOLUTION_WIDTH || mode_setting.vertical_active > MAX_VIRTIOGPU_RESOLUTION_HEIGHT)
        return Error::from_errno(ENOTSUP);

    auto& info = m_display_info;
    info.rect = {
        .x = 0,
        .y = 0,
        .width = (u32)mode_setting.horizontal_active,
        .height = (u32)mode_setting.vertical_active,
    };

    TRY(m_graphics_adapter->mode_set_resolution({}, *this, mode_setting.horizontal_active, mode_setting.vertical_active));

    if (m_console)
        m_console->set_resolution(info.rect.width, info.rect.height, info.rect.width * sizeof(u32));
    DisplayConnector::ModeSetting mode_set {
        .horizontal_stride = info.rect.width * sizeof(u32),
        .pixel_clock_in_khz = 0, // Note: There's no pixel clock in paravirtualized hardware
        .horizontal_active = info.rect.width,
        .horizontal_front_porch_pixels = 0, // Note: There's no horizontal_front_porch_pixels in paravirtualized hardware
        .horizontal_sync_time_pixels = 0,   // Note: There's no horizontal_sync_time_pixels in paravirtualized hardware
        .horizontal_blank_pixels = 0,       // Note: There's no horizontal_blank_pixels in paravirtualized hardware
        .vertical_active = info.rect.height,
        .vertical_front_porch_lines = 0, // Note: There's no vertical_front_porch_lines in paravirtualized hardware
        .vertical_sync_time_lines = 0,   // Note: There's no vertical_sync_time_lines in paravirtualized hardware
        .vertical_blank_lines = 0,       // Note: There's no vertical_blank_lines in paravirtualized hardware
        .horizontal_offset = 0,
        .vertical_offset = 0,
    };
    m_current_mode_setting = mode_set;

    m_display_info.enabled = 1;
    return {};
}
ErrorOr<void> VirtIODisplayConnector::set_safe_mode_setting()
{
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

ErrorOr<void> VirtIODisplayConnector::set_y_offset(size_t)
{
    // NOTE (FIXME?): We don't do double buffering because when using double buffering,
    // perfomance visually looks terrible (everything look sluggish) compared to not using it,
    // so until we figure out why (and we might not figure this and double buffering is simply not needed)
    // this happens, we simply don't support it.
    return Error::from_errno(ENOTSUP);
}
ErrorOr<void> VirtIODisplayConnector::unblank()
{
    return Error::from_errno(ENOTIMPL);
}

ErrorOr<void> VirtIODisplayConnector::flush_rectangle(size_t buffer_index, FBRect const& rect)
{
    VERIFY(m_flushing_lock.is_locked());
    if (!is_valid_buffer_index(buffer_index))
        return Error::from_errno(EINVAL);
    SpinlockLocker locker(m_graphics_adapter->operation_lock());
    Graphics::VirtIOGPU::Protocol::Rect dirty_rect {
        .x = rect.x,
        .y = rect.y,
        .width = rect.width,
        .height = rect.height
    };

    TRY(m_graphics_adapter->transfer_framebuffer_data_to_host({}, *this, dirty_rect, true));
    // Flushing directly to screen
    TRY(flush_displayed_image(dirty_rect, true));
    return {};
}

ErrorOr<void> VirtIODisplayConnector::flush_first_surface()
{
    VERIFY(m_flushing_lock.is_locked());
    SpinlockLocker locker(m_graphics_adapter->operation_lock());
    Graphics::VirtIOGPU::Protocol::Rect dirty_rect {
        .x = 0,
        .y = 0,
        .width = m_display_info.rect.width,
        .height = m_display_info.rect.height
    };

    TRY(m_graphics_adapter->transfer_framebuffer_data_to_host({}, *this, dirty_rect, true));
    // Flushing directly to screen
    TRY(flush_displayed_image(dirty_rect, true));
    return {};
}

void VirtIODisplayConnector::enable_console()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_console);
    m_console->enable();
}

void VirtIODisplayConnector::disable_console()
{
    VERIFY(m_control_lock.is_locked());
    VERIFY(m_console);
    m_console->disable();
}

void VirtIODisplayConnector::set_edid_bytes(Badge<VirtIOGraphicsAdapter>, Array<u8, 128> const& edid_bytes)
{
    DisplayConnector::set_edid_bytes(edid_bytes);
}

Graphics::VirtIOGPU::Protocol::DisplayInfoResponse::Display VirtIODisplayConnector::display_information(Badge<VirtIOGraphicsAdapter>) const
{
    return m_display_info;
}

void VirtIODisplayConnector::clear_to_black()
{
    size_t width = m_display_info.rect.width;
    size_t height = m_display_info.rect.height;
    u8* data = framebuffer_data();
    for (size_t i = 0; i < width * height; ++i) {
        data[4 * i + 0] = 0x00;
        data[4 * i + 1] = 0x00;
        data[4 * i + 2] = 0x00;
        data[4 * i + 3] = 0xff;
    }
}

ErrorOr<void> VirtIODisplayConnector::flush_displayed_image(Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect, bool main_buffer)
{
    VERIFY(m_graphics_adapter->operation_lock().is_locked());
    TRY(m_graphics_adapter->flush_displayed_image({}, *this, dirty_rect, main_buffer));
    return {};
}

void VirtIODisplayConnector::set_dirty_displayed_rect(Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect, bool main_buffer)
{
    VERIFY(m_graphics_adapter->operation_lock().is_locked());
    m_graphics_adapter->set_dirty_displayed_rect({}, *this, dirty_rect, main_buffer);
}

}
