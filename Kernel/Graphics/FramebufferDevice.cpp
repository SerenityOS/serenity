/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/Singleton.h>
#include <Kernel/Debug.h>
#include <Kernel/Graphics/FramebufferDevice.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Process.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/TypedMapping.h>
#include <LibC/errno_numbers.h>
#include <LibC/sys/ioctl_numbers.h>

#include <Kernel/Panic.h>

#define MAX_RESOLUTION_WIDTH 4096
#define MAX_RESOLUTION_HEIGHT 2160

namespace Kernel {

NonnullRefPtr<FramebufferDevice> FramebufferDevice::create(const GraphicsDevice& adapter, size_t output_port_index, PhysicalAddress paddr, size_t width, size_t height, size_t pitch)
{
    return adopt_ref(*new FramebufferDevice(adapter, output_port_index, paddr, width, height, pitch));
}

KResultOr<Region*> FramebufferDevice::mmap(Process& process, FileDescription&, const Range& range, u64 offset, int prot, bool shared)
{
    ScopedSpinLock lock(m_activation_lock);
    REQUIRE_PROMISE(video);
    if (!shared)
        return ENODEV;
    if (offset != 0)
        return ENXIO;
    if (range.size() != page_round_up(framebuffer_size_in_bytes()))
        return EOVERFLOW;

    auto vmobject = AnonymousVMObject::create_for_physical_range(m_framebuffer_address, page_round_up(framebuffer_size_in_bytes()));
    if (!vmobject)
        return ENOMEM;
    m_userspace_real_framebuffer_vmobject = vmobject;

    m_real_framebuffer_vmobject = AnonymousVMObject::create_for_physical_range(m_framebuffer_address, page_round_up(framebuffer_size_in_bytes()));
    m_swapped_framebuffer_vmobject = AnonymousVMObject::create_with_size(page_round_up(framebuffer_size_in_bytes()), AllocationStrategy::AllocateNow);
    m_real_framebuffer_region = MM.allocate_kernel_region_with_vmobject(*m_real_framebuffer_vmobject, page_round_up(framebuffer_size_in_bytes()), "Framebuffer", Region::Access::Read | Region::Access::Write);
    m_swapped_framebuffer_region = MM.allocate_kernel_region_with_vmobject(*m_swapped_framebuffer_vmobject, page_round_up(framebuffer_size_in_bytes()), "Framebuffer Swap (Blank)", Region::Access::Read | Region::Access::Write);

    RefPtr<VMObject> chosen_vmobject;
    if (m_graphical_writes_enabled) {
        chosen_vmobject = m_real_framebuffer_vmobject;
    } else {
        chosen_vmobject = m_swapped_framebuffer_vmobject;
    }
    auto result = process.space().allocate_region_with_vmobject(
        range,
        chosen_vmobject.release_nonnull(),
        0,
        "Framebuffer",
        prot,
        shared);
    if (!result.is_error()) {
        m_userspace_framebuffer_region = result.value();
    }
    return result;
}

void FramebufferDevice::deactivate_writes()
{
    ScopedSpinLock lock(m_activation_lock);
    if (!m_userspace_framebuffer_region)
        return;
    memcpy(m_swapped_framebuffer_region->vaddr().as_ptr(), m_real_framebuffer_region->vaddr().as_ptr(), page_round_up(framebuffer_size_in_bytes()));
    auto vmobject = m_swapped_framebuffer_vmobject;
    m_userspace_framebuffer_region->set_vmobject(vmobject.release_nonnull());
    m_userspace_framebuffer_region->remap();
    m_graphical_writes_enabled = false;
}
void FramebufferDevice::activate_writes()
{
    ScopedSpinLock lock(m_activation_lock);
    if (!m_userspace_framebuffer_region || !m_real_framebuffer_vmobject)
        return;
    // restore the image we had in the void area
    // FIXME: if we happen to have multiple Framebuffers that are writing to that location
    // we will experience glitches...
    memcpy(m_real_framebuffer_region->vaddr().as_ptr(), m_swapped_framebuffer_region->vaddr().as_ptr(), page_round_up(framebuffer_size_in_bytes()));
    auto vmobject = m_userspace_real_framebuffer_vmobject;
    m_userspace_framebuffer_region->set_vmobject(vmobject.release_nonnull());
    m_userspace_framebuffer_region->remap();
    m_graphical_writes_enabled = true;
}

String FramebufferDevice::device_name() const
{
    return String::formatted("fb{}", minor());
}

UNMAP_AFTER_INIT void FramebufferDevice::initialize()
{
    m_real_framebuffer_vmobject = AnonymousVMObject::create_for_physical_range(m_framebuffer_address, page_round_up(framebuffer_size_in_bytes()));
    VERIFY(m_real_framebuffer_vmobject);
    m_real_framebuffer_region = MM.allocate_kernel_region_with_vmobject(*m_real_framebuffer_vmobject, page_round_up(framebuffer_size_in_bytes()), "Framebuffer", Region::Access::Read | Region::Access::Write);
    VERIFY(m_real_framebuffer_region);
    m_swapped_framebuffer_vmobject = AnonymousVMObject::create_with_size(page_round_up(framebuffer_size_in_bytes()), AllocationStrategy::AllocateNow);
    VERIFY(m_swapped_framebuffer_vmobject);
    m_swapped_framebuffer_region = MM.allocate_kernel_region_with_vmobject(*m_swapped_framebuffer_vmobject, page_round_up(framebuffer_size_in_bytes()), "Framebuffer Swap (Blank)", Region::Access::Read | Region::Access::Write);
    VERIFY(m_swapped_framebuffer_region);
}

UNMAP_AFTER_INIT FramebufferDevice::FramebufferDevice(const GraphicsDevice& adapter, size_t output_port_index, PhysicalAddress addr, size_t width, size_t height, size_t pitch)
    : BlockDevice(29, GraphicsManagement::the().allocate_minor_device_number())
    , m_framebuffer_address(addr)
    , m_framebuffer_pitch(pitch)
    , m_framebuffer_width(width)
    , m_framebuffer_height(height)
    , m_output_port_index(output_port_index)
    , m_graphics_adapter(adapter)
{
    VERIFY(!m_framebuffer_address.is_null());
    VERIFY(m_framebuffer_pitch);
    VERIFY(m_framebuffer_width);
    VERIFY(m_framebuffer_height);
    dbgln("Framebuffer {}: address={}, pitch={}, width={}, height={}", minor(), addr, pitch, width, height);
}

size_t FramebufferDevice::framebuffer_size_in_bytes() const
{
    if (m_graphics_adapter->double_framebuffering_capable())
        return m_framebuffer_pitch * m_framebuffer_height * 2;
    return m_framebuffer_pitch * m_framebuffer_height;
}

int FramebufferDevice::ioctl(FileDescription&, unsigned request, FlatPtr arg)
{
    REQUIRE_PROMISE(video);
    switch (request) {
    case FB_IOCTL_GET_SIZE_IN_BYTES: {
        auto* out = (size_t*)arg;
        size_t value = framebuffer_size_in_bytes();
        if (!copy_to_user(out, &value))
            return -EFAULT;
        return 0;
    }
    case FB_IOCTL_GET_BUFFER: {
        auto* index = (int*)arg;
        int value = m_y_offset == 0 ? 0 : 1;
        if (!copy_to_user(index, &value))
            return -EFAULT;
        if (!m_graphics_adapter->double_framebuffering_capable())
            return -ENOTIMPL;
        return 0;
    }
    case FB_IOCTL_SET_BUFFER: {
        if (arg != 0 && arg != 1)
            return -EINVAL;
        if (!m_graphics_adapter->double_framebuffering_capable())
            return -ENOTIMPL;
        m_graphics_adapter->set_y_offset(m_output_port_index, arg == 0 ? 0 : m_framebuffer_height);
        return 0;
    }
    case FB_IOCTL_GET_RESOLUTION: {
        auto* user_resolution = (FBResolution*)arg;
        FBResolution resolution;
        resolution.pitch = m_framebuffer_pitch;
        resolution.width = m_framebuffer_width;
        resolution.height = m_framebuffer_height;
        if (!copy_to_user(user_resolution, &resolution))
            return -EFAULT;
        return 0;
    }
    case FB_IOCTL_SET_RESOLUTION: {
        auto* user_resolution = (FBResolution*)arg;
        FBResolution resolution;
        if (!copy_from_user(&resolution, user_resolution))
            return -EFAULT;
        if (resolution.width > MAX_RESOLUTION_WIDTH || resolution.height > MAX_RESOLUTION_HEIGHT)
            return -EINVAL;

        if (!m_graphics_adapter->modesetting_capable()) {
            resolution.pitch = m_framebuffer_pitch;
            resolution.width = m_framebuffer_width;
            resolution.height = m_framebuffer_height;
            if (!copy_to_user(user_resolution, &resolution))
                return -EFAULT;
            return -ENOTIMPL;
        }

        if (!m_graphics_adapter->try_to_set_resolution(m_output_port_index, resolution.width, resolution.height)) {
            m_framebuffer_pitch = m_framebuffer_width * sizeof(u32);
            dbgln_if(FRAMEBUFFER_DEVICE_DEBUG, "Reverting resolution: [{}x{}]", m_framebuffer_width, m_framebuffer_height);
            // Note: We try to revert everything back, and if it doesn't work, just assert.
            if (!m_graphics_adapter->try_to_set_resolution(m_output_port_index, m_framebuffer_width, m_framebuffer_height)) {
                VERIFY_NOT_REACHED();
            }
            resolution.pitch = m_framebuffer_pitch;
            resolution.width = m_framebuffer_width;
            resolution.height = m_framebuffer_height;
            if (!copy_to_user(user_resolution, &resolution))
                return -EFAULT;
            return -EINVAL;
        }
        m_framebuffer_width = resolution.width;
        m_framebuffer_height = resolution.height;
        m_framebuffer_pitch = m_framebuffer_width * sizeof(u32);

        dbgln_if(FRAMEBUFFER_DEVICE_DEBUG, "New resolution: [{}x{}]", m_framebuffer_width, m_framebuffer_height);
        resolution.pitch = m_framebuffer_pitch;
        resolution.width = m_framebuffer_width;
        resolution.height = m_framebuffer_height;
        if (!copy_to_user(user_resolution, &resolution))
            return -EFAULT;
        return 0;
    }
    default:
        return -EINVAL;
    };
}

}
