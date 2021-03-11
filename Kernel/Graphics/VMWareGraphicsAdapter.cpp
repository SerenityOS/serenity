/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
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

#include <AK/Atomic.h>
#include <AK/Checked.h>
#include <AK/Singleton.h>
#include <Kernel/Debug.h>
#include <Kernel/Graphics/VMWareFramebufferDevice.h>
#include <Kernel/Graphics/VMWareGraphicsAdapter.h>
#include <Kernel/IO.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/Process.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/TypedMapping.h>
#include <LibC/errno_numbers.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

namespace VMWare {
struct [[gnu::packed]] SVGAGuestPtr {
    u32 gmr_id;
    u32 offset;
};

struct [[gnu::packed]] SVGAGuestImage {
    SVGAGuestPtr ptr;
    u32 pitch;
};

struct [[gnu::packed]] SVGAScreenObject {
    u32 size;
    u32 id;
    u32 flags;

    u32 width;
    u32 height;

    i32 x;
    i32 y;

    SVGAGuestImage backing_store;
    u32 clone_count;
};

enum SVGARegisterIndex {
    ID = 0,
    Enable = 1,
    Width = 2,
    Height = 3,
    MaxWidth = 4,
    MaxHeight = 5,
    Depth = 6,
    BitsPerPixel = 7,
    Pseudocolor = 8,
    RedMask = 9,
    GreenMask = 10,
    BlueMask = 11,
    BytesPerline = 12,
    FramebufferStart = 13,
    FramebufferOffset = 14,
    VRAMSize = 15,
    FramebufferSize = 16,
    Capbilities = 17,
    MemoryStart = 18,
    MemorySize = 19,
    ConfigurationDone = 20,
    Sync = 21,
    Busy = 22,
    GuestID = 23,
    ScratchSize = 29,
    MemoryRegistersCount = 30,
    DisplaysCount = 31,
    PitchLock = 32,
    IRQMask = 33,
};

struct [[gnu::packed]] FIFORegisters {
    u32 min;
    u32 max;
    u32 next_cmd;
    u32 stop;
    u32 commands[];
};

}

#define SVGA_MAGIC 0x900000UL
#define SVGA_MAKE_ID(ver) (SVGA_MAGIC << 8 | (ver))
#define SVGA_ID_WITHOUT_MAGIC(ver) ((ver)&0xFF)
#define SVGA_ID_0 SVGA_MAKE_ID(0)
#define SVGA_ID_1 SVGA_MAKE_ID(1)
#define SVGA_ID_2 SVGA_MAKE_ID(2)

UNMAP_AFTER_INIT NonnullRefPtr<VMWareGraphicsAdapter> VMWareGraphicsAdapter::initialize(PCI::Address address)
{
    return adopt(*new VMWareGraphicsAdapter(address));
}

UNMAP_AFTER_INIT VMWareGraphicsAdapter::VMWareGraphicsAdapter(PCI::Address address)
    : PCI::DeviceController(address)
    , m_svga_control(PCI::get_BAR0(address) & 0xfffffff0 & ~1)
    , m_fifo_region(PCI::get_BAR2(address) & 0xfffffff0)
    , m_framebuffer_address(PCI::get_BAR1(address) & 0xfffffff0)
{
    dmesgln("VMWare SVGA @ {} - {}", address, m_svga_control);
    if (!negotiate_version()) {
        dmesgln("VMWare SVGA @ {}, failed to negotiate version", address);
        return;
    }

    m_operable = true;

    // Mask all IRQs
    write_svga_register(VMWare::SVGARegisterIndex::IRQMask, 0);
    //// clear all pending IRQs if there are any
    m_svga_control.offset(8).out<u32>(0xFF);

    write_svga_register(VMWare::SVGARegisterIndex::GuestID, 1);

    auto fifo_registers = map_typed_writable<volatile VMWare::FIFORegisters>(m_fifo_region);
    fifo_registers->min = 16;
    fifo_registers->max = 16 + (10 * 1024);
    fifo_registers->next_cmd = 16;
    fifo_registers->stop = 16;

    set_safe_resolution();
}

UNMAP_AFTER_INIT bool VMWareGraphicsAdapter::negotiate_version()
{
    write_svga_register(VMWare::SVGARegisterIndex::ID, SVGA_ID_2);
    dmesgln("VMWare SVGA @ {} - Version ID {:x}", pci_address(), SVGA_ID_WITHOUT_MAGIC(read_svga_register(VMWare::SVGARegisterIndex::ID)));
    return read_svga_register(VMWare::SVGARegisterIndex::ID) == SVGA_ID_2;
}

void VMWareGraphicsAdapter::write_svga_register(u16 index, u32 value)
{
    m_svga_control.out<u32>(index);
    m_svga_control.offset(1).out<u32>(value);
}
u32 VMWareGraphicsAdapter::read_svga_register(u16 index)
{
    m_svga_control.out<u32>(index);
    return m_svga_control.offset(1).in<u32>();
}

UNMAP_AFTER_INIT void VMWareGraphicsAdapter::enumerate_displays()
{
    if (m_operable)
        m_framebuffer = VMWareFramebufferDevice::create(*this, m_framebuffer_address, 0, 0, 0);
}

void VMWareGraphicsAdapter::set_safe_resolution()
{
    set_resolution(1024, 768);
}

void VMWareGraphicsAdapter::set_resolution_registers(size_t width, size_t height)
{
    write_svga_register(VMWare::SVGARegisterIndex::Enable, 0);
    write_svga_register(VMWare::SVGARegisterIndex::ID, 0);
    write_svga_register(VMWare::SVGARegisterIndex::Width, width);
    write_svga_register(VMWare::SVGARegisterIndex::Height, height);
    write_svga_register(VMWare::SVGARegisterIndex::BitsPerPixel, 32);
    write_svga_register(VMWare::SVGARegisterIndex::Enable, 1);
    write_svga_register(VMWare::SVGARegisterIndex::ConfigurationDone, 1);
}

bool VMWareGraphicsAdapter::try_to_set_resolution(size_t width, size_t height)
{
    dbgln_if(BXVGA_DEBUG, "VMWareGraphicsAdapter resolution test - {}x{}", width, height);
    set_resolution_registers(width, height);
    return validate_setup_resolution(width, height);
}

bool VMWareGraphicsAdapter::set_resolution(size_t width, size_t height)
{
    if (Checked<size_t>::multiplication_would_overflow(width, height, sizeof(u32)))
        return false;

    if (!try_to_set_resolution(width, height))
        return false;

    dbgln("VMWareGraphicsAdapter: resolution set to {}x{}", width, height);
    return true;
}

bool VMWareGraphicsAdapter::validate_setup_resolution(size_t, size_t)
{
    return true;
}

void VMWareGraphicsAdapter::set_y_offset(size_t)
{
}

}
