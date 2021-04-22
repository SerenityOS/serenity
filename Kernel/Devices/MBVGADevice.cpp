/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/MBVGADevice.h>
#include <Kernel/Process.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibC/errno_numbers.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

static MBVGADevice* s_the;

MBVGADevice& MBVGADevice::the()
{
    return *s_the;
}

UNMAP_AFTER_INIT MBVGADevice::MBVGADevice(PhysicalAddress addr, size_t pitch, size_t width, size_t height)
    : BlockDevice(29, 0)
    , m_framebuffer_address(addr)
    , m_framebuffer_pitch(pitch)
    , m_framebuffer_width(width)
    , m_framebuffer_height(height)
{
    dbgln("MBVGADevice address={}, pitch={}, width={}, height={}", addr, pitch, width, height);
    s_the = this;
}

KResultOr<Region*> MBVGADevice::mmap(Process& process, FileDescription&, const Range& range, u64 offset, int prot, bool shared)
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
        "MBVGA Framebuffer",
        prot,
        shared);
}

int MBVGADevice::ioctl(FileDescription&, unsigned request, FlatPtr arg)
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
        int value = 0;
        if (!copy_to_user(index, &value))
            return -EFAULT;
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

String MBVGADevice::device_name() const
{
    return String::formatted("fb{}", minor());
}

}
