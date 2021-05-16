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

namespace Kernel {

KResultOr<Region*> FramebufferDevice::mmap(Process& process, FileDescription&, const Range& range, u64 offset, int prot, bool shared)
{
    REQUIRE_PROMISE(video);
    if (!shared)
        return ENODEV;
    if (offset != 0)
        return ENXIO;
    if (range.size() != page_round_up(framebuffer_size_in_bytes()))
        return EOVERFLOW;

    // FIXME: We rely on the fact that only the WindowServer will mmap the framebuffer
    // and only once when starting to work with it. If other program wants to do so, we need to fix this.
    VERIFY(!m_userspace_framebuffer_region);
    VERIFY(!m_userspace_real_framebuffer_vmobject);

    auto vmobject = AnonymousVMObject::create_for_physical_range(m_framebuffer_address, page_round_up(framebuffer_size_in_bytes()));
    if (!vmobject)
        return ENOMEM;
    m_userspace_real_framebuffer_vmobject = vmobject;

    auto result = process.space().allocate_region_with_vmobject(
        range,
        vmobject.release_nonnull(),
        0,
        "Framebuffer",
        prot,
        shared);
    if (!result.is_error()) {
        m_userspace_framebuffer_region = result.value();
    }
    return result;
}

void FramebufferDevice::dectivate_writes()
{
    ScopedSpinLock lock(m_activation_lock);
    if (!m_userspace_framebuffer_region)
        return;
    memcpy(m_swapped_framebuffer_region->vaddr().as_ptr(), m_real_framebuffer_region->vaddr().as_ptr(), page_round_up(framebuffer_size_in_bytes()));
    auto vmobject = m_swapped_framebuffer_vmobject;
    m_userspace_framebuffer_region->set_vmobject(vmobject.release_nonnull());
    m_userspace_framebuffer_region->remap();
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
}

String FramebufferDevice::device_name() const
{
    return String::formatted("fb{}", minor());
}

UNMAP_AFTER_INIT void FramebufferDevice::initialize()
{
    m_real_framebuffer_vmobject = AnonymousVMObject::create_for_physical_range(m_framebuffer_address, page_round_up(framebuffer_size_in_bytes()));
    VERIFY(m_real_framebuffer_vmobject);
    m_real_framebuffer_region = MM.allocate_kernel_region_with_vmobject(*m_real_framebuffer_vmobject, framebuffer_size_in_bytes(), "Framebuffer", Region::Access::Read | Region::Access::Write);
    VERIFY(m_real_framebuffer_region);
    m_swapped_framebuffer_vmobject = AnonymousVMObject::create_with_size(page_round_up(framebuffer_size_in_bytes()), AllocationStrategy::AllocateNow);
    VERIFY(m_swapped_framebuffer_vmobject);
    m_swapped_framebuffer_region = MM.allocate_kernel_region_with_vmobject(*m_swapped_framebuffer_vmobject, page_round_up(framebuffer_size_in_bytes()), "Framebuffer Swap (Blank)", Region::Access::Read | Region::Access::Write);
    VERIFY(m_swapped_framebuffer_region);
}

UNMAP_AFTER_INIT FramebufferDevice::FramebufferDevice(PhysicalAddress addr, size_t pitch, size_t width, size_t height)
    : BlockDevice(29, GraphicsManagement::the().allocate_minor_device_number())
    , m_framebuffer_address(addr)
    , m_framebuffer_pitch(pitch)
    , m_framebuffer_width(width)
    , m_framebuffer_height(height)
{
    VERIFY(!m_framebuffer_address.is_null());
    VERIFY(m_framebuffer_pitch);
    VERIFY(m_framebuffer_width);
    VERIFY(m_framebuffer_height);
    dbgln("Framebuffer {}: address={}, pitch={}, width={}, height={}", minor(), addr, pitch, width, height);
}

bool FramebufferDevice::set_resolution(size_t, size_t, size_t)
{
    VERIFY_NOT_REACHED();
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
        return -ENOTIMPL;
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
