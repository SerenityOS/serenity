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

#include <Kernel/Devices/BXVGADevice.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/Process.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibBareMetal/IO.h>
#include <LibC/errno_numbers.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

#define MAX_RESOLUTION_WIDTH 4096
#define MAX_RESOLUTION_HEIGHT 2160

#define VBE_DISPI_IOPORT_INDEX 0x01CE
#define VBE_DISPI_IOPORT_DATA 0x01CF

#define VBE_DISPI_INDEX_ID 0x0
#define VBE_DISPI_INDEX_XRES 0x1
#define VBE_DISPI_INDEX_YRES 0x2
#define VBE_DISPI_INDEX_BPP 0x3
#define VBE_DISPI_INDEX_ENABLE 0x4
#define VBE_DISPI_INDEX_BANK 0x5
#define VBE_DISPI_INDEX_VIRT_WIDTH 0x6
#define VBE_DISPI_INDEX_VIRT_HEIGHT 0x7
#define VBE_DISPI_INDEX_X_OFFSET 0x8
#define VBE_DISPI_INDEX_Y_OFFSET 0x9
#define VBE_DISPI_DISABLED 0x00
#define VBE_DISPI_ENABLED 0x01
#define VBE_DISPI_LFB_ENABLED 0x40

static BXVGADevice* s_the;

BXVGADevice& BXVGADevice::the()
{
    return *s_the;
}

BXVGADevice::BXVGADevice()
    : BlockDevice(29, 0)
{
    s_the = this;
    m_framebuffer_address = PhysicalAddress(find_framebuffer_address());
}

void BXVGADevice::set_register(u16 index, u16 data)
{
    IO::out16(VBE_DISPI_IOPORT_INDEX, index);
    IO::out16(VBE_DISPI_IOPORT_DATA, data);
}

void BXVGADevice::set_resolution(int width, int height)
{
    m_framebuffer_pitch = width * sizeof(u32);
    m_framebuffer_width = width;
    m_framebuffer_height = height;

    set_register(VBE_DISPI_INDEX_ENABLE, VBE_DISPI_DISABLED);
    set_register(VBE_DISPI_INDEX_XRES, (u16)width);
    set_register(VBE_DISPI_INDEX_YRES, (u16)height);
    set_register(VBE_DISPI_INDEX_VIRT_WIDTH, (u16)width);
    set_register(VBE_DISPI_INDEX_VIRT_HEIGHT, (u16)height * 2);
    set_register(VBE_DISPI_INDEX_BPP, 32);
    set_register(VBE_DISPI_INDEX_ENABLE, VBE_DISPI_ENABLED | VBE_DISPI_LFB_ENABLED);
    set_register(VBE_DISPI_INDEX_BANK, 0);

    dbg() << "BXVGADevice resolution set to " << m_framebuffer_width << "x" << m_framebuffer_height << " (pitch=" << m_framebuffer_pitch << ")";
}

void BXVGADevice::set_y_offset(int y_offset)
{
    ASSERT(y_offset == 0 || y_offset == m_framebuffer_height);
    m_y_offset = y_offset;
    set_register(VBE_DISPI_INDEX_Y_OFFSET, (u16)y_offset);
}

u32 BXVGADevice::find_framebuffer_address()
{
    // NOTE: The QEMU card has the same PCI ID as the Bochs one.
    static const PCI::ID bochs_vga_id = { 0x1234, 0x1111 };
    static const PCI::ID virtualbox_vga_id = { 0x80ee, 0xbeef };
    u32 framebuffer_address = 0;
    PCI::enumerate_all([&framebuffer_address](const PCI::Address& address, PCI::ID id) {
        if (id == bochs_vga_id || id == virtualbox_vga_id) {
            framebuffer_address = PCI::get_BAR0(address) & 0xfffffff0;
            kprintf("BXVGA: framebuffer @ P%x\n", framebuffer_address);
        }
    });
    return framebuffer_address;
}

KResultOr<Region*> BXVGADevice::mmap(Process& process, FileDescription&, VirtualAddress preferred_vaddr, size_t offset, size_t size, int prot)
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
        "BXVGA Framebuffer",
        prot);
    dbg() << "BXVGADevice: mmap with size " << region->size() << " at " << region->vaddr();
    ASSERT(region);
    return region;
}

int BXVGADevice::ioctl(FileDescription&, unsigned request, unsigned arg)
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
        *index = m_y_offset == 0 ? 0 : 1;
        return 0;
    }
    case FB_IOCTL_SET_BUFFER: {
        if (arg != 0 && arg != 1)
            return -EINVAL;
        set_y_offset(arg == 0 ? 0 : m_framebuffer_height);
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
        if (resolution->width > MAX_RESOLUTION_WIDTH || resolution->height > MAX_RESOLUTION_HEIGHT)
            return -EINVAL;
        set_resolution(resolution->width, resolution->height);
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
