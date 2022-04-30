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

NonnullRefPtr<VirtIODisplayConnector> VirtIODisplayConnector::must_create(VirtIOGraphicsAdapter& graphics_adapter, Graphics::VirtIOGPU::ScanoutID scanout_id)
{
    auto device_or_error = DeviceManagement::try_create_device<VirtIODisplayConnector>(graphics_adapter, scanout_id);
    VERIFY(!device_or_error.is_error());
    auto connector = device_or_error.release_value();
    connector->initialize_console();
    return connector;
}

VirtIODisplayConnector::VirtIODisplayConnector(VirtIOGraphicsAdapter& graphics_adapter, Graphics::VirtIOGPU::ScanoutID scanout_id)
    : DisplayConnector()
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
    TRY(create_framebuffer());
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
        m_current_buffer = &m_main_buffer;
    else if (y == m_display_info.rect.height)
        m_current_buffer = &m_back_buffer;
    else
        return Error::from_errno(EINVAL);
    return {};
}
ErrorOr<void> VirtIODisplayConnector::unblank()
{
    return Error::from_errno(ENOTIMPL);
}

ErrorOr<size_t> VirtIODisplayConnector::write_to_first_surface(u64 offset, UserOrKernelBuffer const& buffer, size_t length)
{
    VERIFY(m_control_lock.is_locked());
    if (offset + length > (m_buffer_size * 2))
        return Error::from_errno(EOVERFLOW);
    if (offset < m_buffer_size && (offset + length) > (m_buffer_size))
        return Error::from_errno(EOVERFLOW);
    if (offset < m_buffer_size) {
        TRY(buffer.read(m_main_buffer.framebuffer_data + offset, 0, length));
    } else {
        TRY(buffer.read(m_back_buffer.framebuffer_data + offset - m_buffer_size, 0, length));
    }

    return length;
}

ErrorOr<void> VirtIODisplayConnector::flush_rectangle(size_t buffer_index, FBRect const& rect)
{
    VERIFY(m_flushing_lock.is_locked());
    SpinlockLocker locker(m_graphics_adapter->operation_lock());
    Graphics::VirtIOGPU::Protocol::Rect dirty_rect {
        .x = rect.x,
        .y = rect.y,
        .width = rect.width,
        .height = rect.height
    };

    auto& buffer = buffer_from_index(buffer_index);
    transfer_framebuffer_data_to_host(dirty_rect, buffer);
    if (&buffer == m_current_buffer) {
        // Flushing directly to screen
        flush_displayed_image(dirty_rect, buffer);
        buffer.dirty_rect = {};
    } else {
        if (buffer.dirty_rect.width == 0 || buffer.dirty_rect.height == 0) {
            buffer.dirty_rect = dirty_rect;
        } else {
            auto current_dirty_right = buffer.dirty_rect.x + buffer.dirty_rect.width;
            auto current_dirty_bottom = buffer.dirty_rect.y + buffer.dirty_rect.height;
            buffer.dirty_rect.x = min(buffer.dirty_rect.x, dirty_rect.x);
            buffer.dirty_rect.y = min(buffer.dirty_rect.y, dirty_rect.y);
            buffer.dirty_rect.width = max(current_dirty_right, dirty_rect.x + dirty_rect.width) - buffer.dirty_rect.x;
            buffer.dirty_rect.height = max(current_dirty_bottom, dirty_rect.y + dirty_rect.height) - buffer.dirty_rect.y;
        }
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
    auto& buffer = buffer_from_index(0);
    transfer_framebuffer_data_to_host(dirty_rect, buffer);
    if (&buffer == m_current_buffer) {
        // Flushing directly to screen
        flush_displayed_image(dirty_rect, buffer);
        buffer.dirty_rect = {};
    } else {
        if (buffer.dirty_rect.width == 0 || buffer.dirty_rect.height == 0) {
            buffer.dirty_rect = dirty_rect;
        } else {
            auto current_dirty_right = buffer.dirty_rect.x + buffer.dirty_rect.width;
            auto current_dirty_bottom = buffer.dirty_rect.y + buffer.dirty_rect.height;
            buffer.dirty_rect.x = min(buffer.dirty_rect.x, dirty_rect.x);
            buffer.dirty_rect.y = min(buffer.dirty_rect.y, dirty_rect.y);
            buffer.dirty_rect.width = max(current_dirty_right, dirty_rect.x + dirty_rect.width) - buffer.dirty_rect.x;
            buffer.dirty_rect.height = max(current_dirty_bottom, dirty_rect.y + dirty_rect.height) - buffer.dirty_rect.y;
        }
    }
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

void VirtIODisplayConnector::clear_to_black(Buffer& buffer)
{
    size_t width = m_display_info.rect.width;
    size_t height = m_display_info.rect.height;
    u8* data = buffer.framebuffer_data;
    for (size_t i = 0; i < width * height; ++i) {
        data[4 * i + 0] = 0x00;
        data[4 * i + 1] = 0x00;
        data[4 * i + 2] = 0x00;
        data[4 * i + 3] = 0xff;
    }
}

void VirtIODisplayConnector::draw_ntsc_test_pattern(Buffer& buffer)
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
    u8* data = buffer.framebuffer_data;
    // Draw NTSC test card
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
    dbgln_if(VIRTIO_DEBUG, "Finish drawing the pattern");
}

u8* VirtIODisplayConnector::framebuffer_data()
{
    return m_current_buffer->framebuffer_data;
}

ErrorOr<void> VirtIODisplayConnector::create_framebuffer()
{
    SpinlockLocker locker(m_graphics_adapter->operation_lock());
    // First delete any existing framebuffers to free the memory first
    m_framebuffer = nullptr;
    m_framebuffer_sink_vmobject = nullptr;

    // Allocate frame buffer for both front and back
    m_buffer_size = calculate_framebuffer_size(m_display_info.rect.width, m_display_info.rect.height);
    auto region_name = TRY(KString::formatted("VirtGPU FrameBuffer #{}", m_scanout_id.value()));
    m_framebuffer = TRY(MM.allocate_kernel_region(m_buffer_size * 2, region_name->view(), Memory::Region::Access::ReadWrite, AllocationStrategy::AllocateNow));
    auto write_sink_page = TRY(MM.allocate_user_physical_page(Memory::MemoryManager::ShouldZeroFill::No));
    auto num_needed_pages = m_framebuffer->vmobject().page_count();

    NonnullRefPtrVector<Memory::PhysicalPage> pages;
    for (auto i = 0u; i < num_needed_pages; ++i) {
        TRY(pages.try_append(write_sink_page));
    }
    m_framebuffer_sink_vmobject = TRY(Memory::AnonymousVMObject::try_create_with_physical_pages(pages.span()));

    m_current_buffer = &buffer_from_index(m_last_set_buffer_index.load());
    create_buffer(m_main_buffer, 0, m_buffer_size);
    create_buffer(m_back_buffer, m_buffer_size, m_buffer_size);

    return {};
}

void VirtIODisplayConnector::set_edid_bytes(Badge<VirtIOGraphicsAdapter>, Array<u8, 128> const& edid_bytes)
{
    DisplayConnector::set_edid_bytes(edid_bytes);
}

Graphics::VirtIOGPU::Protocol::DisplayInfoResponse::Display VirtIODisplayConnector::display_information(Badge<VirtIOGraphicsAdapter>)
{
    return m_display_info;
}

void VirtIODisplayConnector::create_buffer(Buffer& buffer, size_t framebuffer_offset, size_t framebuffer_size)
{
    VERIFY(m_graphics_adapter->operation_lock().is_locked());
    buffer.framebuffer_offset = framebuffer_offset;
    buffer.framebuffer_data = m_framebuffer->vaddr().as_ptr() + framebuffer_offset;

    // 1. Create BUFFER using VIRTIO_GPU_CMD_RESOURCE_CREATE_2D
    if (buffer.resource_id.value() != 0)
        m_graphics_adapter->delete_resource(buffer.resource_id);
    buffer.resource_id = m_graphics_adapter->create_2d_resource(m_display_info.rect);

    // 2. Attach backing storage using  VIRTIO_GPU_CMD_RESOURCE_ATTACH_BACKING
    m_graphics_adapter->ensure_backing_storage(buffer.resource_id, *m_framebuffer, buffer.framebuffer_offset, framebuffer_size);
    // 3. Use VIRTIO_GPU_CMD_SET_SCANOUT to link the framebuffer to a display scanout.
    if (&buffer == m_current_buffer)
        m_graphics_adapter->set_scanout_resource(m_scanout_id, buffer.resource_id, m_display_info.rect);
    // 4. Render our test pattern
    draw_ntsc_test_pattern(buffer);
    // 5. Use VIRTIO_GPU_CMD_TRANSFER_TO_HOST_2D to update the host resource from guest memory.
    transfer_framebuffer_data_to_host(m_display_info.rect, buffer);
    // 6. Use VIRTIO_GPU_CMD_RESOURCE_FLUSH to flush the updated resource to the display.
    if (&buffer == m_current_buffer)
        flush_displayed_image(m_display_info.rect, buffer);

    // Make sure we constrain the existing dirty rect (if any)
    if (buffer.dirty_rect.width != 0 || buffer.dirty_rect.height != 0) {
        auto dirty_right = buffer.dirty_rect.x + buffer.dirty_rect.width;
        auto dirty_bottom = buffer.dirty_rect.y + buffer.dirty_rect.height;
        buffer.dirty_rect.width = min(dirty_right, m_display_info.rect.x + m_display_info.rect.width) - buffer.dirty_rect.x;
        buffer.dirty_rect.height = min(dirty_bottom, m_display_info.rect.y + m_display_info.rect.height) - buffer.dirty_rect.y;
    }

    m_display_info.enabled = 1;
}

void VirtIODisplayConnector::transfer_framebuffer_data_to_host(Graphics::VirtIOGPU::Protocol::Rect const& rect, Buffer& buffer)
{
    m_graphics_adapter->transfer_framebuffer_data_to_host(m_scanout_id, buffer.resource_id, rect);
}

void VirtIODisplayConnector::flush_dirty_window(Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect, Buffer& buffer)
{
    m_graphics_adapter->flush_dirty_rectangle(m_scanout_id, buffer.resource_id, dirty_rect);
}

void VirtIODisplayConnector::flush_displayed_image(Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect, Buffer& buffer)
{
    m_graphics_adapter->flush_displayed_image(buffer.resource_id, dirty_rect);
}

}
