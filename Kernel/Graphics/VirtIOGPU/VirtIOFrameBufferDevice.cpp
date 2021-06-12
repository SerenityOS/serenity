/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/VirtIOGPU/VirtIOFrameBufferDevice.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel::Graphics {

VirtIOFrameBufferDevice::VirtIOFrameBufferDevice(RefPtr<VirtIOGPU> virtio_gpu)
    : BlockDevice(29, GraphicsManagement::the().allocate_minor_device_number())
    , m_gpu(virtio_gpu)
{
    auto write_sink_page = MM.allocate_user_physical_page(MemoryManager::ShouldZeroFill::No).release_nonnull();
    auto num_needed_pages = m_gpu->framebuffer_vm_object().page_count();
    NonnullRefPtrVector<PhysicalPage> pages;
    for (auto i = 0u; i < num_needed_pages; ++i) {
        pages.append(write_sink_page);
    }
    m_framebuffer_sink_vmobject = AnonymousVMObject::create_with_physical_pages(move(pages));
}

VirtIOFrameBufferDevice::~VirtIOFrameBufferDevice()
{
}

int VirtIOFrameBufferDevice::ioctl(FileDescription&, unsigned request, FlatPtr arg)
{
    REQUIRE_PROMISE(video);
    switch (request) {
    case FB_IOCTL_GET_SIZE_IN_BYTES: {
        auto* out = (size_t*)arg;
        size_t value = m_gpu->framebuffer_size_in_bytes();
        if (!copy_to_user(out, &value))
            return -EFAULT;
        return 0;
    }
    case FB_IOCTL_SET_RESOLUTION: {
        auto* user_resolution = (FBResolution*)arg;
        FBResolution resolution;
        if (!copy_from_user(&resolution, user_resolution))
            return -EFAULT;
        if (!m_gpu->try_to_set_resolution(resolution.width, resolution.height))
            return -EINVAL;
        resolution.pitch = m_gpu->framebuffer_pitch();
        if (!copy_to_user(user_resolution, &resolution))
            return -EFAULT;
        return 0;
    }
    case FB_IOCTL_GET_RESOLUTION: {
        auto* user_resolution = (FBResolution*)arg;
        FBResolution resolution;
        resolution.pitch = m_gpu->framebuffer_pitch();
        resolution.width = m_gpu->framebuffer_width();
        resolution.height = m_gpu->framebuffer_height();
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
            m_gpu->flush_dirty_window(dirty_rect);
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
    if (range.size() != page_round_up(m_gpu->framebuffer_size_in_bytes()))
        return EOVERFLOW;

    // We only allow one process to map the region
    if (m_userspace_mmap_region)
        return ENOMEM;

    auto vmobject = m_are_writes_active ? m_gpu->framebuffer_vm_object().clone() : m_framebuffer_sink_vmobject;
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
        region->set_vmobject(m_gpu->framebuffer_vm_object());
        region->remap();
    }
}

}
