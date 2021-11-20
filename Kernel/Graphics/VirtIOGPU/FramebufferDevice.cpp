/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/VirtIOGPU/FramebufferDevice.h>
#include <Kernel/Graphics/VirtIOGPU/GraphicsAdapter.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel::Graphics::VirtIOGPU {

RefPtr<GraphicsAdapter> FramebufferDevice::adapter() const
{
    auto adapter = m_graphics_adapter.strong_ref();
    // FIXME: Propagate error gracefully
    VERIFY(adapter);
    return static_cast<GraphicsAdapter&>(*adapter);
}

ErrorOr<size_t> FramebufferDevice::buffer_length(size_t head) const
{
    // Note: This FramebufferDevice class doesn't support multihead setup.
    // We take care to verify this at the GenericFramebufferDevice::ioctl method
    // so if we happen to accidentally have a value different than 0, assert.
    VERIFY(head == 0);
    MutexLocker locker(m_resolution_lock);
    return display_info().rect.width * display_info().rect.height * 4;
}
ErrorOr<size_t> FramebufferDevice::pitch(size_t head) const
{
    // Note: This FramebufferDevice class doesn't support multihead setup.
    // We take care to verify this at the GenericFramebufferDevice::ioctl method
    // so if we happen to accidentally have a value different than 0, assert.
    VERIFY(head == 0);
    MutexLocker locker(m_resolution_lock);
    return display_info().rect.width * 4;
}
ErrorOr<size_t> FramebufferDevice::height(size_t head) const
{
    // Note: This FramebufferDevice class doesn't support multihead setup.
    // We take care to verify this at the GenericFramebufferDevice::ioctl method
    // so if we happen to accidentally have a value different than 0, assert.
    VERIFY(head == 0);
    MutexLocker locker(m_resolution_lock);
    return display_info().rect.height;
}
ErrorOr<size_t> FramebufferDevice::width(size_t head) const
{
    // Note: This FramebufferDevice class doesn't support multihead setup.
    // We take care to verify this at the GenericFramebufferDevice::ioctl method
    // so if we happen to accidentally have a value different than 0, assert.
    VERIFY(head == 0);
    MutexLocker locker(m_resolution_lock);
    return display_info().rect.width;
}
ErrorOr<size_t> FramebufferDevice::vertical_offset(size_t head) const
{
    // Note: This FramebufferDevice class doesn't support multihead setup.
    // We take care to verify this at the GenericFramebufferDevice::ioctl method
    // so if we happen to accidentally have a value different than 0, assert.
    VERIFY(head == 0);
    return 0;
}
ErrorOr<bool> FramebufferDevice::vertical_offsetted(size_t head) const
{
    // Note: This FramebufferDevice class doesn't support multihead setup.
    // We take care to verify this at the GenericFramebufferDevice::ioctl method
    // so if we happen to accidentally have a value different than 0, assert.
    VERIFY(head == 0);
    return false;
}

ErrorOr<void> FramebufferDevice::set_head_resolution(size_t head, size_t width, size_t height, size_t)
{
    // Note: This class doesn't support multihead setup (yet!).
    // We take care to verify this at the GenericFramebufferDevice::ioctl method
    // so if we happen to accidentally have a value different than 0, assert.
    VERIFY(head == 0);
    if (width > MAX_VIRTIOGPU_RESOLUTION_WIDTH || height > MAX_VIRTIOGPU_RESOLUTION_HEIGHT)
        return Error::from_errno(ENOTSUP);

    auto& info = display_info();

    MutexLocker locker(adapter()->operation_lock());

    info.rect = {
        .x = 0,
        .y = 0,
        .width = (u32)width,
        .height = (u32)height,
    };

    // FIXME: Would be nice to be able to return ErrorOr here.
    TRY(create_framebuffer());
    return {};
}
ErrorOr<void> FramebufferDevice::set_head_buffer(size_t, bool)
{
    return Error::from_errno(ENOTSUP);
}
ErrorOr<void> FramebufferDevice::flush_head_buffer(size_t)
{
    // Note: This class doesn't support flushing.
    // We take care to verify this at the GenericFramebufferDevice::ioctl method
    // so if we happen to accidentally reach this code, assert.
    VERIFY_NOT_REACHED();
}
ErrorOr<void> FramebufferDevice::flush_rectangle(size_t buffer_index, FBRect const& rect)
{
    MutexLocker locker(adapter()->operation_lock());
    Protocol::Rect dirty_rect {
        .x = rect.x,
        .y = rect.y,
        .width = rect.width,
        .height = rect.height
    };
    // FIXME: Find a better ErrorOr<void> here.
    if (!m_are_writes_active)
        return Error::from_errno(EIO);
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

FramebufferDevice::FramebufferDevice(GraphicsAdapter const& adapter, ScanoutID scanout)
    : GenericFramebufferDevice(adapter)
    , m_scanout(scanout)
{
    if (display_info().enabled) {
        // FIXME: This should be in a place where we can handle allocation failures.
        auto result = create_framebuffer();
        VERIFY(!result.is_error());
    }
}

FramebufferDevice::~FramebufferDevice()
{
}

ErrorOr<void> FramebufferDevice::create_framebuffer()
{
    // First delete any existing framebuffers to free the memory first
    m_framebuffer = nullptr;
    m_framebuffer_sink_vmobject = nullptr;

    // Allocate frame buffer for both front and back
    auto& info = display_info();
    m_buffer_size = calculate_framebuffer_size(info.rect.width, info.rect.height);
    m_framebuffer = TRY(MM.allocate_kernel_region(m_buffer_size * 2, String::formatted("VirtGPU FrameBuffer #{}", m_scanout.value()), Memory::Region::Access::ReadWrite, AllocationStrategy::AllocateNow));
    auto write_sink_page = MM.allocate_user_physical_page(Memory::MemoryManager::ShouldZeroFill::No).release_nonnull();
    auto num_needed_pages = m_framebuffer->vmobject().page_count();

    NonnullRefPtrVector<Memory::PhysicalPage> pages;
    for (auto i = 0u; i < num_needed_pages; ++i) {
        pages.append(write_sink_page);
    }
    m_framebuffer_sink_vmobject = TRY(Memory::AnonymousVMObject::try_create_with_physical_pages(pages.span()));

    MutexLocker locker(adapter()->operation_lock());
    m_current_buffer = &buffer_from_index(m_last_set_buffer_index.load());
    create_buffer(m_main_buffer, 0, m_buffer_size);
    create_buffer(m_back_buffer, m_buffer_size, m_buffer_size);

    return {};
}

void FramebufferDevice::create_buffer(Buffer& buffer, size_t framebuffer_offset, size_t framebuffer_size)
{
    buffer.framebuffer_offset = framebuffer_offset;
    buffer.framebuffer_data = m_framebuffer->vaddr().as_ptr() + framebuffer_offset;

    auto& info = display_info();

    // 1. Create BUFFER using VIRTIO_GPU_CMD_RESOURCE_CREATE_2D
    if (buffer.resource_id.value() != 0)
        adapter()->delete_resource(buffer.resource_id);
    buffer.resource_id = adapter()->create_2d_resource(info.rect);

    // 2. Attach backing storage using  VIRTIO_GPU_CMD_RESOURCE_ATTACH_BACKING
    adapter()->ensure_backing_storage(buffer.resource_id, *m_framebuffer, buffer.framebuffer_offset, framebuffer_size);
    // 3. Use VIRTIO_GPU_CMD_SET_SCANOUT to link the framebuffer to a display scanout.
    if (&buffer == m_current_buffer)
        adapter()->set_scanout_resource(m_scanout.value(), buffer.resource_id, info.rect);
    // 4. Render our test pattern
    draw_ntsc_test_pattern(buffer);
    // 5. Use VIRTIO_GPU_CMD_TRANSFER_TO_HOST_2D to update the host resource from guest memory.
    transfer_framebuffer_data_to_host(info.rect, buffer);
    // 6. Use VIRTIO_GPU_CMD_RESOURCE_FLUSH to flush the updated resource to the display.
    if (&buffer == m_current_buffer)
        flush_displayed_image(info.rect, buffer);

    // Make sure we constrain the existing dirty rect (if any)
    if (buffer.dirty_rect.width != 0 || buffer.dirty_rect.height != 0) {
        auto dirty_right = buffer.dirty_rect.x + buffer.dirty_rect.width;
        auto dirty_bottom = buffer.dirty_rect.y + buffer.dirty_rect.height;
        buffer.dirty_rect.width = min(dirty_right, info.rect.x + info.rect.width) - buffer.dirty_rect.x;
        buffer.dirty_rect.height = min(dirty_bottom, info.rect.y + info.rect.height) - buffer.dirty_rect.y;
    }

    info.enabled = 1;
}

Protocol::DisplayInfoResponse::Display const& FramebufferDevice::display_info() const
{
    return adapter()->display_info(m_scanout);
}

Protocol::DisplayInfoResponse::Display& FramebufferDevice::display_info()
{
    return adapter()->display_info(m_scanout);
}

void FramebufferDevice::transfer_framebuffer_data_to_host(Protocol::Rect const& rect, Buffer& buffer)
{
    adapter()->transfer_framebuffer_data_to_host(m_scanout, buffer.resource_id, rect);
}

void FramebufferDevice::flush_dirty_window(Protocol::Rect const& dirty_rect, Buffer& buffer)
{
    adapter()->flush_dirty_rectangle(m_scanout, buffer.resource_id, dirty_rect);
}

void FramebufferDevice::flush_displayed_image(Protocol::Rect const& dirty_rect, Buffer& buffer)
{
    adapter()->flush_displayed_image(buffer.resource_id, dirty_rect);
}

void FramebufferDevice::set_buffer(int buffer_index)
{
    auto& buffer = buffer_index == 0 ? m_main_buffer : m_back_buffer;
    MutexLocker locker(adapter()->operation_lock());
    if (&buffer == m_current_buffer)
        return;
    m_current_buffer = &buffer;
    adapter()->set_scanout_resource(m_scanout.value(), buffer.resource_id, display_info().rect);
    adapter()->flush_displayed_image(buffer.resource_id, buffer.dirty_rect); // QEMU SDL backend requires this (as per spec)
    buffer.dirty_rect = {};
}

ErrorOr<Memory::Region*> FramebufferDevice::mmap(Process& process, OpenFileDescription&, Memory::VirtualRange const& range, u64 offset, int prot, bool shared)
{
    REQUIRE_PROMISE(video);
    if (!shared)
        return ENODEV;
    if (offset != 0 || !m_framebuffer)
        return ENXIO;
    if (range.size() > m_framebuffer->size())
        return EOVERFLOW;

    // We only allow one process to map the region
    if (m_userspace_mmap_region)
        return ENOMEM;

    RefPtr<Memory::VMObject> vmobject;
    if (m_are_writes_active) {
        vmobject = TRY(m_framebuffer->vmobject().try_clone());
    } else {
        vmobject = m_framebuffer_sink_vmobject;
        if (vmobject.is_null())
            return ENOMEM;
    }

    m_userspace_mmap_region = TRY(process.address_space().allocate_region_with_vmobject(
        range,
        vmobject.release_nonnull(),
        0,
        "VirtIOGPU Framebuffer",
        prot,
        shared));

    return m_userspace_mmap_region.unsafe_ptr();
}

void FramebufferDevice::deactivate_writes()
{
    m_are_writes_active = false;
    if (m_userspace_mmap_region) {
        auto* region = m_userspace_mmap_region.unsafe_ptr();
        auto maybe_vm_object = m_framebuffer_sink_vmobject->try_clone();
        // FIXME: Would be nice to be able to return a ErrorOr<void> here.
        VERIFY(!maybe_vm_object.is_error());
        region->set_vmobject(maybe_vm_object.release_value());
        region->remap();
    }
    set_buffer(0);
    clear_to_black(buffer_from_index(0));
}

void FramebufferDevice::activate_writes()
{
    m_are_writes_active = true;
    auto last_set_buffer_index = m_last_set_buffer_index.load();
    if (m_userspace_mmap_region) {
        auto* region = m_userspace_mmap_region.unsafe_ptr();
        region->set_vmobject(m_framebuffer->vmobject());
        region->remap();
    }
    set_buffer(last_set_buffer_index);
}

void FramebufferDevice::clear_to_black(Buffer& buffer)
{
    auto& info = display_info();
    size_t width = info.rect.width;
    size_t height = info.rect.height;
    u8* data = buffer.framebuffer_data;
    for (size_t i = 0; i < width * height; ++i) {
        data[4 * i + 0] = 0x00;
        data[4 * i + 1] = 0x00;
        data[4 * i + 2] = 0x00;
        data[4 * i + 3] = 0xff;
    }
}

void FramebufferDevice::draw_ntsc_test_pattern(Buffer& buffer)
{
    static constexpr u8 colors[12][4] = {
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
    auto& info = display_info();
    size_t width = info.rect.width;
    size_t height = info.rect.height;
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

u8* FramebufferDevice::framebuffer_data()
{
    return m_current_buffer->framebuffer_data;
}

}
