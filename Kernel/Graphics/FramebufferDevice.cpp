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

    auto vmobject = AnonymousVMObject::create_for_physical_range(m_framebuffer_address, framebuffer_size_in_bytes());
    if (!vmobject)
        return ENOMEM;
    return process.space().allocate_region_with_vmobject(
        range,
        vmobject.release_nonnull(),
        0,
        "Framebuffer",
        prot,
        shared);
}

String FramebufferDevice::device_name() const
{
    return String::formatted("fb{}", minor());
}

UNMAP_AFTER_INIT FramebufferDevice::FramebufferDevice(PhysicalAddress addr, size_t pitch, size_t width, size_t height)
    : BlockDevice(29, GraphicsManagement::the().current_minor_number())
    , m_framebuffer_address(addr)
    , m_framebuffer_pitch(pitch)
    , m_framebuffer_width(width)
    , m_framebuffer_height(height)
{
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
