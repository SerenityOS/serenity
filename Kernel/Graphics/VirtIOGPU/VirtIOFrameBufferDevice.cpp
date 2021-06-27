/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/VirtIOGPU/VirtIOFrameBufferDevice.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel::Graphics {

VirtIOFrameBufferDevice::VirtIOFrameBufferDevice(VirtIOGPU& virtio_gpu, VirtIOGPUScanoutID scanout)
    : BlockDevice(29, GraphicsManagement::the().allocate_minor_device_number())
    , m_gpu(virtio_gpu)
    , m_scanout(scanout)
{
    if (display_info().enabled) {
        Locker locker(m_gpu.operation_lock());
        create_framebuffer();
    }
}

VirtIOFrameBufferDevice::~VirtIOFrameBufferDevice()
{
}

void VirtIOFrameBufferDevice::create_framebuffer()
{
    auto& info = display_info();

    size_t buffer_length = page_round_up(calculate_framebuffer_size(info.rect.width, info.rect.height));

    // First delete any existing framebuffers to free the memory first
    m_framebuffer = nullptr;
    m_framebuffer_sink_vmobject = nullptr;

    // 1. Allocate frame buffer
    m_framebuffer = MM.allocate_kernel_region(buffer_length, String::formatted("VirtGPU FrameBuffer #{}", m_scanout.value()), Region::Access::Read | Region::Access::Write, AllocationStrategy::AllocateNow);
    auto write_sink_page = MM.allocate_user_physical_page(MemoryManager::ShouldZeroFill::No).release_nonnull();
    auto num_needed_pages = m_framebuffer->vmobject().page_count();
    NonnullRefPtrVector<PhysicalPage> pages;
    for (auto i = 0u; i < num_needed_pages; ++i) {
        pages.append(write_sink_page);
    }
    m_framebuffer_sink_vmobject = AnonymousVMObject::create_with_physical_pages(move(pages));

    // 2. Create BUFFER using VIRTIO_GPU_CMD_RESOURCE_CREATE_2D
    if (m_resource_id.value() != 0)
        m_gpu.delete_resource(m_resource_id);
    m_resource_id = m_gpu.create_2d_resource(info.rect);

    // 3. Attach backing storage using  VIRTIO_GPU_CMD_RESOURCE_ATTACH_BACKING
    m_gpu.ensure_backing_storage(*m_framebuffer, buffer_length, m_resource_id);
    // 4. Use VIRTIO_GPU_CMD_SET_SCANOUT to link the framebuffer to a display scanout.
    m_gpu.set_scanout_resource(m_scanout.value(), m_resource_id, info.rect);
    // 5. Render our test pattern
    draw_ntsc_test_pattern();
    // 6. Use VIRTIO_GPU_CMD_TRANSFER_TO_HOST_2D to update the host resource from guest memory.
    transfer_framebuffer_data_to_host(info.rect);
    // 7. Use VIRTIO_GPU_CMD_RESOURCE_FLUSH to flush the updated resource to the display.
    flush_displayed_image(info.rect);

    info.enabled = 1;
}

VirtIOGPURespDisplayInfo::VirtIOGPUDisplayOne const& VirtIOFrameBufferDevice::display_info() const
{
    return m_gpu.display_info(m_scanout);
}

VirtIOGPURespDisplayInfo::VirtIOGPUDisplayOne& VirtIOFrameBufferDevice::display_info()
{
    return m_gpu.display_info(m_scanout);
}

size_t VirtIOFrameBufferDevice::size_in_bytes() const
{
    auto& info = display_info();
    return info.rect.width * info.rect.height * sizeof(u32);
}

void VirtIOFrameBufferDevice::flush_dirty_window(VirtIOGPURect const& dirty_rect)
{
    m_gpu.flush_dirty_window(m_scanout, dirty_rect, m_resource_id);
}

void VirtIOFrameBufferDevice::transfer_framebuffer_data_to_host(VirtIOGPURect const& rect)
{
    m_gpu.transfer_framebuffer_data_to_host(m_scanout, rect, m_resource_id);
}

void VirtIOFrameBufferDevice::flush_displayed_image(VirtIOGPURect const& dirty_rect)
{
    m_gpu.flush_displayed_image(dirty_rect, m_resource_id);
}

bool VirtIOFrameBufferDevice::try_to_set_resolution(size_t width, size_t height)
{
    if (width > MAX_VIRTIOGPU_RESOLUTION_WIDTH || height > MAX_VIRTIOGPU_RESOLUTION_HEIGHT)
        return false;

    auto& info = display_info();

    Locker locker(m_gpu.operation_lock());

    info.rect = {
        .x = 0,
        .y = 0,
        .width = (u32)width,
        .height = (u32)height,
    };
    create_framebuffer();
    return true;
}

int VirtIOFrameBufferDevice::ioctl(FileDescription&, unsigned request, FlatPtr arg)
{
    REQUIRE_PROMISE(video);
    switch (request) {
    case FB_IOCTL_GET_SIZE_IN_BYTES: {
        auto* out = (size_t*)arg;
        size_t value = size_in_bytes();
        if (!copy_to_user(out, &value))
            return -EFAULT;
        return 0;
    }
    case FB_IOCTL_SET_RESOLUTION: {
        auto* user_resolution = (FBResolution*)arg;
        FBResolution resolution;
        if (!copy_from_user(&resolution, user_resolution))
            return -EFAULT;
        if (!try_to_set_resolution(resolution.width, resolution.height))
            return -EINVAL;
        resolution.pitch = pitch();
        if (!copy_to_user(user_resolution, &resolution))
            return -EFAULT;
        return 0;
    }
    case FB_IOCTL_GET_RESOLUTION: {
        auto* user_resolution = (FBResolution*)arg;
        FBResolution resolution;
        resolution.pitch = pitch();
        resolution.width = width();
        resolution.height = height();
        if (!copy_to_user(user_resolution, &resolution))
            return -EFAULT;
        return 0;
    }
    case FB_IOCTL_FLUSH_BUFFER: {
        FBRect user_dirty_rect;
        if (!copy_from_user(&user_dirty_rect, (FBRect*)arg))
            return -EFAULT;
        VirtIOGPURect dirty_rect {
            .x = user_dirty_rect.x,
            .y = user_dirty_rect.y,
            .width = user_dirty_rect.width,
            .height = user_dirty_rect.height
        };
        if (m_are_writes_active)
            flush_dirty_window(dirty_rect);
        return 0;
    }
    default:
        return -EINVAL;
    };
}

KResultOr<Region*> VirtIOFrameBufferDevice::mmap(Process& process, FileDescription&, const Range& range, u64 offset, int prot, bool shared)
{
    REQUIRE_PROMISE(video);
    if (!shared)
        return ENODEV;
    if (offset != 0)
        return ENXIO;
    if (range.size() != page_round_up(size_in_bytes()))
        return EOVERFLOW;

    // We only allow one process to map the region
    if (m_userspace_mmap_region)
        return ENOMEM;

    auto vmobject = m_are_writes_active ? m_framebuffer->vmobject().clone() : m_framebuffer_sink_vmobject;
    if (vmobject.is_null())
        return ENOMEM;

    auto result = process.space().allocate_region_with_vmobject(
        range,
        vmobject.release_nonnull(),
        0,
        "VirtIOGPU Framebuffer",
        prot,
        shared);
    if (result.is_error())
        return result;
    m_userspace_mmap_region = result.value();
    return result;
}

void VirtIOFrameBufferDevice::deactivate_writes()
{
    m_are_writes_active = false;
    if (m_userspace_mmap_region) {
        auto* region = m_userspace_mmap_region.unsafe_ptr();
        auto vm_object = m_framebuffer_sink_vmobject->clone();
        VERIFY(vm_object);
        region->set_vmobject(vm_object.release_nonnull());
        region->remap();
    }
}

void VirtIOFrameBufferDevice::activate_writes()
{
    m_are_writes_active = true;
    if (m_userspace_mmap_region) {
        auto* region = m_userspace_mmap_region.unsafe_ptr();
        region->set_vmobject(m_framebuffer->vmobject());
        region->remap();
    }
}

void VirtIOFrameBufferDevice::clear_to_black()
{
    auto& info = display_info();
    size_t width = info.rect.width;
    size_t height = info.rect.height;
    u8* data = m_framebuffer->vaddr().as_ptr();
    for (size_t i = 0; i < width * height; ++i) {
        data[4 * i + 0] = 0x00;
        data[4 * i + 1] = 0x00;
        data[4 * i + 2] = 0x00;
        data[4 * i + 3] = 0xff;
    }
}

void VirtIOFrameBufferDevice::draw_ntsc_test_pattern()
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
    u8* data = m_framebuffer->vaddr().as_ptr();
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

u8* VirtIOFrameBufferDevice::framebuffer_data()
{
    return m_framebuffer->vaddr().as_ptr();
}

}
