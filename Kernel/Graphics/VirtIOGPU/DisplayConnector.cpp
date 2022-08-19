/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/VirGL.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/VirtIOGPU/Console.h>
#include <Kernel/Graphics/VirtIOGPU/DisplayConnector.h>
#include <Kernel/Graphics/VirtIOGPU/GraphicsAdapter.h>
#include <Kernel/Graphics/VirtIOGPU/Protocol.h>
#include <Kernel/Random.h>

namespace Kernel {

NonnullLockRefPtr<VirtIODisplayConnector> VirtIODisplayConnector::must_create(VirtIOGraphicsAdapter& graphics_adapter, Graphics::VirtIOGPU::ScanoutID scanout_id)
{
    auto device_or_error = DeviceManagement::try_create_device<VirtIODisplayConnector>(graphics_adapter, scanout_id);
    VERIFY(!device_or_error.is_error());
    auto connector = device_or_error.release_value();
    connector->initialize_console();
    return connector;
}

static_assert((MAX_VIRTIOGPU_RESOLUTION_WIDTH * MAX_VIRTIOGPU_RESOLUTION_HEIGHT * sizeof(u32) * 2) % PAGE_SIZE == 0);

VirtIODisplayConnector::VirtIODisplayConnector(VirtIOGraphicsAdapter& graphics_adapter, Graphics::VirtIOGPU::ScanoutID scanout_id)
    : DisplayConnector((MAX_VIRTIOGPU_RESOLUTION_WIDTH * MAX_VIRTIOGPU_RESOLUTION_HEIGHT * sizeof(u32) * 2), false)
    , m_graphics_adapter(graphics_adapter)
    , m_scanout_id(scanout_id)
{
}

void VirtIODisplayConnector::initialize_console()
{
    m_console = Kernel::Graphics::VirtIOGPU::Console::initialize(*this);
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

ErrorOr<void> VirtIODisplayConnector::set_y_offset(size_t y)
{
    VERIFY(m_control_lock.is_locked());
    if (y == 0)
        m_last_set_buffer_index.store(0);
    else if (y == m_display_info.rect.height)
        m_last_set_buffer_index.store(1);
    else
        return Error::from_errno(EINVAL);
    return {};
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

    bool main_buffer = (buffer_index == 0);
    m_graphics_adapter->transfer_framebuffer_data_to_host({}, *this, dirty_rect, main_buffer);
    if (m_last_set_buffer_index.load() == buffer_index) {
        // Flushing directly to screen
        flush_displayed_image(dirty_rect, main_buffer);
    } else {
        set_dirty_displayed_rect(dirty_rect, main_buffer);
    }
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

    auto current_buffer_index = m_last_set_buffer_index.load();
    VERIFY(is_valid_buffer_index(current_buffer_index));

    bool main_buffer = (current_buffer_index == 0);
    m_graphics_adapter->transfer_framebuffer_data_to_host({}, *this, dirty_rect, main_buffer);
    // Flushing directly to screen
    flush_displayed_image(dirty_rect, main_buffer);
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

void VirtIODisplayConnector::draw_ntsc_test_pattern(Badge<VirtIOGraphicsAdapter>)
{
    constexpr u8 colors[12][4] = {
        { 0xff, 0xff, 0xff, 0xff }, // White
        { 0x00, 0xff, 0xff, 0xff }, // Primary + Composite colors
        { 0xff, 0xff, 0x00, 0xff },
        { 0x00, 0xff, 0x00, 0xff },
        { 0xff, 0x00, 0xff, 0xff },
        { 0x00, 0x00, 0xff, 0xff },
        { 0xff, 0x00, 0x00, 0xff },
        { 0xba, 0x01, 0x5f, 0xff }, // Dark blue
        { 0x8d, 0x3d, 0x00, 0xff }, // Purple
        { 0x22, 0x22, 0x22, 0xff }, // Shades of gray
        { 0x10, 0x10, 0x10, 0xff },
        { 0x00, 0x00, 0x00, 0xff },
    };
    size_t width = m_display_info.rect.width;
    size_t height = m_display_info.rect.height;
    u8* data = framebuffer_data();
    // Draw NTSC test card
    for (size_t i = 0; i < 2; ++i) {
        for (size_t y = 0; y < height; ++y) {
            for (size_t x = 0; x < width; ++x) {
                size_t color = 0;
                if (3 * y < 2 * height) {
                    // Top 2/3 of image is 7 vertical stripes of color spectrum
                    color = (7 * x) / width;
                } else if (4 * y < 3 * height) {
                    // 2/3 mark to 3/4 mark  is backwards color spectrum alternating with black
                    auto segment = (7 * x) / width;
                    color = segment % 2 ? 10 : 6 - segment;
                } else {
                    if (28 * x < 5 * width) {
                        color = 8;
                    } else if (28 * x < 10 * width) {
                        color = 0;
                    } else if (28 * x < 15 * width) {
                        color = 7;
                    } else if (28 * x < 20 * width) {
                        color = 10;
                    } else if (7 * x < 6 * width) {
                        // Grayscale gradient
                        color = 26 - ((21 * x) / width);
                    } else {
                        // Solid black
                        color = 10;
                    }
                }
                u8* pixel = &data[4 * (y * width + x)];
                for (int i = 0; i < 4; ++i) {
                    pixel[i] = colors[color][i];
                }
            }
        }
        data = data + (width * height * sizeof(u32));
    }
    dbgln_if(VIRTIO_DEBUG, "Finish drawing the pattern");
}

void VirtIODisplayConnector::flush_displayed_image(Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect, bool main_buffer)
{
    VERIFY(m_graphics_adapter->operation_lock().is_locked());
    m_graphics_adapter->flush_displayed_image({}, *this, dirty_rect, main_buffer);
}

void VirtIODisplayConnector::set_dirty_displayed_rect(Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect, bool main_buffer)
{
    VERIFY(m_graphics_adapter->operation_lock().is_locked());
    m_graphics_adapter->set_dirty_displayed_rect({}, *this, dirty_rect, main_buffer);
}

}
