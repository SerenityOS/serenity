/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Graphics/FramebufferDevice.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>
#include <LibC/errno_numbers.h>
#include <LibC/sys/ioctl_numbers.h>

#define MAX_RESOLUTION_WIDTH 4096
#define MAX_RESOLUTION_HEIGHT 2160

namespace Kernel {

NonnullRefPtr<FramebufferDevice> FramebufferDevice::create(const GraphicsDevice& adapter, size_t output_port_index, PhysicalAddress paddr, size_t width, size_t height, size_t pitch)
{
    auto framebuffer_device_or_error = DeviceManagement::try_create_device<FramebufferDevice>(adapter, output_port_index, paddr, width, height, pitch);
    // FIXME: Find a way to propagate errors
    VERIFY(!framebuffer_device_or_error.is_error());
    return framebuffer_device_or_error.release_value();
}

KResultOr<Memory::Region*> FramebufferDevice::mmap(Process& process, OpenFileDescription&, Memory::VirtualRange const& range, u64 offset, int prot, bool shared)
{
    SpinlockLocker lock(m_activation_lock);
    REQUIRE_PROMISE(video);
    if (!shared)
        return ENODEV;
    if (offset != 0)
        return ENXIO;
    if (range.size() != Memory::page_round_up(framebuffer_size_in_bytes()))
        return EOVERFLOW;

    m_userspace_real_framebuffer_vmobject = TRY(Memory::AnonymousVMObject::try_create_for_physical_range(m_framebuffer_address, Memory::page_round_up(framebuffer_size_in_bytes())));
    m_real_framebuffer_vmobject = TRY(Memory::AnonymousVMObject::try_create_for_physical_range(m_framebuffer_address, Memory::page_round_up(framebuffer_size_in_bytes())));
    m_swapped_framebuffer_vmobject = TRY(Memory::AnonymousVMObject::try_create_with_size(Memory::page_round_up(framebuffer_size_in_bytes()), AllocationStrategy::AllocateNow));
    m_real_framebuffer_region = TRY(MM.allocate_kernel_region_with_vmobject(*m_real_framebuffer_vmobject, Memory::page_round_up(framebuffer_size_in_bytes()), "Framebuffer", Memory::Region::Access::ReadWrite));
    m_swapped_framebuffer_region = TRY(MM.allocate_kernel_region_with_vmobject(*m_swapped_framebuffer_vmobject, Memory::page_round_up(framebuffer_size_in_bytes()), "Framebuffer Swap (Blank)", Memory::Region::Access::ReadWrite));

    RefPtr<Memory::VMObject> chosen_vmobject;
    if (m_graphical_writes_enabled) {
        chosen_vmobject = m_real_framebuffer_vmobject;
    } else {
        chosen_vmobject = m_swapped_framebuffer_vmobject;
    }
    m_userspace_framebuffer_region = TRY(process.address_space().allocate_region_with_vmobject(
        range,
        chosen_vmobject.release_nonnull(),
        0,
        "Framebuffer",
        prot,
        shared));
    return m_userspace_framebuffer_region;
}

void FramebufferDevice::deactivate_writes()
{
    SpinlockLocker lock(m_activation_lock);
    if (!m_userspace_framebuffer_region)
        return;
    memcpy(m_swapped_framebuffer_region->vaddr().as_ptr(), m_real_framebuffer_region->vaddr().as_ptr(), Memory::page_round_up(framebuffer_size_in_bytes()));
    auto vmobject = m_swapped_framebuffer_vmobject;
    m_userspace_framebuffer_region->set_vmobject(vmobject.release_nonnull());
    m_userspace_framebuffer_region->remap();
    m_graphical_writes_enabled = false;
}
void FramebufferDevice::activate_writes()
{
    SpinlockLocker lock(m_activation_lock);
    if (!m_userspace_framebuffer_region || !m_real_framebuffer_vmobject)
        return;
    // restore the image we had in the void area
    // FIXME: if we happen to have multiple Framebuffers that are writing to that location
    // we will experience glitches...
    memcpy(m_real_framebuffer_region->vaddr().as_ptr(), m_swapped_framebuffer_region->vaddr().as_ptr(), Memory::page_round_up(framebuffer_size_in_bytes()));
    auto vmobject = m_userspace_real_framebuffer_vmobject;
    m_userspace_framebuffer_region->set_vmobject(vmobject.release_nonnull());
    m_userspace_framebuffer_region->remap();
    m_graphical_writes_enabled = true;
}

UNMAP_AFTER_INIT KResult FramebufferDevice::initialize()
{
    // FIXME: Would be nice to be able to unify this with mmap above, but this
    //        function is UNMAP_AFTER_INIT for the time being.
    m_real_framebuffer_vmobject = TRY(Memory::AnonymousVMObject::try_create_for_physical_range(m_framebuffer_address, Memory::page_round_up(framebuffer_size_in_bytes())));
    m_swapped_framebuffer_vmobject = TRY(Memory::AnonymousVMObject::try_create_with_size(Memory::page_round_up(framebuffer_size_in_bytes()), AllocationStrategy::AllocateNow));
    m_real_framebuffer_region = TRY(MM.allocate_kernel_region_with_vmobject(*m_real_framebuffer_vmobject, Memory::page_round_up(framebuffer_size_in_bytes()), "Framebuffer", Memory::Region::Access::ReadWrite));
    m_swapped_framebuffer_region = TRY(MM.allocate_kernel_region_with_vmobject(*m_swapped_framebuffer_vmobject, Memory::page_round_up(framebuffer_size_in_bytes()), "Framebuffer Swap (Blank)", Memory::Region::Access::ReadWrite));
    return KSuccess;
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

KResult FramebufferDevice::ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg)
{
    REQUIRE_PROMISE(video);
    switch (request) {
    case FB_IOCTL_GET_SIZE_IN_BYTES: {
        auto user_size = static_ptr_cast<size_t*>(arg);
        size_t value = framebuffer_size_in_bytes();
        return copy_to_user(user_size, &value);
    }
    case FB_IOCTL_GET_BUFFER: {
        auto user_index = static_ptr_cast<int*>(arg);
        int value = m_y_offset == 0 ? 0 : 1;
        TRY(copy_to_user(user_index, &value));
        if (!m_graphics_adapter->double_framebuffering_capable())
            return ENOTIMPL;
        return KSuccess;
    }
    case FB_IOCTL_SET_BUFFER: {
        auto buffer = static_cast<int>(arg.ptr());
        if (buffer != 0 && buffer != 1)
            return EINVAL;
        if (!m_graphics_adapter->double_framebuffering_capable())
            return ENOTIMPL;
        m_graphics_adapter->set_y_offset(m_output_port_index, buffer == 0 ? 0 : m_framebuffer_height);
        return KSuccess;
    }
    case FB_IOCTL_GET_RESOLUTION: {
        auto user_resolution = static_ptr_cast<FBResolution*>(arg);
        FBResolution resolution {};
        resolution.pitch = m_framebuffer_pitch;
        resolution.width = m_framebuffer_width;
        resolution.height = m_framebuffer_height;
        return copy_to_user(user_resolution, &resolution);
    }
    case FB_IOCTL_SET_RESOLUTION: {
        auto user_resolution = static_ptr_cast<FBResolution*>(arg);
        FBResolution resolution;
        TRY(copy_from_user(&resolution, user_resolution));
        if (resolution.width > MAX_RESOLUTION_WIDTH || resolution.height > MAX_RESOLUTION_HEIGHT)
            return EINVAL;

        if (!m_graphics_adapter->modesetting_capable()) {
            resolution.pitch = m_framebuffer_pitch;
            resolution.width = m_framebuffer_width;
            resolution.height = m_framebuffer_height;
            TRY(copy_to_user(user_resolution, &resolution));
            return ENOTIMPL;
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
            TRY(copy_to_user(user_resolution, &resolution));
            return EINVAL;
        }
        m_framebuffer_width = resolution.width;
        m_framebuffer_height = resolution.height;
        m_framebuffer_pitch = m_framebuffer_width * sizeof(u32);

        dbgln_if(FRAMEBUFFER_DEVICE_DEBUG, "New resolution: [{}x{}]", m_framebuffer_width, m_framebuffer_height);
        resolution.pitch = m_framebuffer_pitch;
        resolution.width = m_framebuffer_width;
        resolution.height = m_framebuffer_height;
        return copy_to_user(user_resolution, &resolution);
    }
    case FB_IOCTL_GET_BUFFER_OFFSET: {
        auto user_buffer_offset = static_ptr_cast<FBBufferOffset*>(arg);
        FBBufferOffset buffer_offset;
        TRY(copy_from_user(&buffer_offset, user_buffer_offset));
        if (buffer_offset.buffer_index != 0 && buffer_offset.buffer_index != 1)
            return EINVAL;
        buffer_offset.offset = (size_t)buffer_offset.buffer_index * m_framebuffer_pitch * m_framebuffer_height;
        return copy_to_user(user_buffer_offset, &buffer_offset);
    }
    case FB_IOCTL_FLUSH_BUFFERS:
        return ENOTSUP;
    default:
        return EINVAL;
    };
}

}
