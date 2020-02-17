/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

MBVGADevice::MBVGADevice(PhysicalAddress addr, int pitch, int width, int height)
    : BlockDevice(29, 0)
    , m_framebuffer_address(addr)
    , m_framebuffer_pitch(pitch)
    , m_framebuffer_width(width)
    , m_framebuffer_height(height)
{
    dbg() << "MBVGADevice address=" << addr << ", pitch=" << pitch << ", width=" << width << ", height=" << height;
    s_the = this;
}

KResultOr<Region*> MBVGADevice::mmap(Process& process, FileDescription&, VirtualAddress preferred_vaddr, size_t offset, size_t size, int prot)
{
    REQUIRE_PROMISE(video);
    ASSERT(offset == 0);
    ASSERT(size == framebuffer_size_in_bytes());
    auto vmobject = AnonymousVMObject::create_for_physical_range(m_framebuffer_address, framebuffer_size_in_bytes());
    if (!vmobject)
        return KResult(-ENOMEM);
    auto* region = process.allocate_region_with_vmobject(
        preferred_vaddr,
        framebuffer_size_in_bytes(),
        vmobject.release_nonnull(),
        0,
        "MBVGA Framebuffer",
        prot);
    dbg() << "MBVGADevice: mmap with size " << region->size() << " at " << region->vaddr();
    ASSERT(region);
    return region;
}

int MBVGADevice::ioctl(FileDescription&, unsigned request, unsigned arg)
{
    REQUIRE_PROMISE(video);
    switch (request) {
    case FB_IOCTL_GET_SIZE_IN_BYTES: {
        auto* out = (size_t*)arg;
        if (!Process::current->validate_write_typed(out))
            return -EFAULT;
        *out = framebuffer_size_in_bytes();
        return 0;
    }
    case FB_IOCTL_GET_BUFFER: {
        auto* index = (int*)arg;
        if (!Process::current->validate_write_typed(index))
            return -EFAULT;
        *index = 0;
        return 0;
    }
    case FB_IOCTL_GET_RESOLUTION: {
        auto* resolution = (FBResolution*)arg;
        if (!Process::current->validate_write_typed(resolution))
            return -EFAULT;
        resolution->pitch = m_framebuffer_pitch;
        resolution->width = m_framebuffer_width;
        resolution->height = m_framebuffer_height;
        return 0;
    }
    case FB_IOCTL_SET_RESOLUTION: {
        auto* resolution = (FBResolution*)arg;
        if (!Process::current->validate_read_typed(resolution) || !Process::current->validate_write_typed(resolution))
            return -EFAULT;
        resolution->pitch = m_framebuffer_pitch;
        resolution->width = m_framebuffer_width;
        resolution->height = m_framebuffer_height;
        return 0;
    }
    default:
        return -EINVAL;
    };
}

}
