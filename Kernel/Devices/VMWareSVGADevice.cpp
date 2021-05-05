/*
 * Copyright (c) 2021, the SerenityOS Developers
 * Copyright (c) 2021, Alexander Richards <electrodeyt@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "VMWareSVGADevice.h"
#include <AK/Singleton.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/PCI/IDs.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibC/errno_numbers.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

VMWareSVGADevice::VMWareSVGADevice(PCI::Address addr)
    : BlockDevice(29, 0)
    , PCI::Device(addr)
{
    PCI::Address address = pci_address();
    // Get the base address of the IO addresses
    m_io_base = PCI::get_BAR0(address) & 0xfffffff0;
    // Get the base address of the pre-allocated (non system ram) framebuffer
    m_framebuffer_address_physical = PhysicalAddress(page_base_of(PCI::get_BAR1(address)));
    m_framebuffer_max_size = PCI::get_BAR_space_size(address, 1);
    // Get the base address of the fifo and map it into kernel space
    // TODO: do we ever need to read from it?
    m_fifo_mapping = MM.allocate_kernel_region(PhysicalAddress(page_base_of(PCI::get_BAR2(address) & 0xfffffff0)), page_round_up(PCI::get_BAR_space_size(address, 2)), "VMWareSVGA Driver FIFO", Region::Access::Read | Region::Access::Write, Region::Cacheable::No);
    m_fifo_addr = m_fifo_mapping->vaddr();
    // Clear framebuffer memory
    {
        auto region = MM.allocate_kernel_region(m_framebuffer_address_physical, page_round_up(m_framebuffer_max_size), {}, Region::Access::Write, Region::Cacheable::No);
        memset(region->vaddr().as_ptr(), 0, m_framebuffer_max_size);
    }
    VERIFY(m_io_base);
    VERIFY(m_framebuffer_address_physical.as_ptr());
    VERIFY(m_fifo_addr.as_ptr());

    // Determine SVGA Version
    // The way this is done is by writing the highest version we support (2) into a device support register,
    // and checking if the value has been changed.
    // If it hasn't, then we found the highest version that both we and the "card" support.
    // If it has, decrement by one and repeat.
    m_device_version = make_version_id(2);
    set_register(IORegister::ID, m_device_version);
    while (get_register(IORegister::ID) != m_device_version && m_device_version >= make_version_id(0)) {
        m_device_version--;
        set_register(IORegister::ID, m_device_version);
    }

    VERIFY(m_device_version >= make_version_id(0));

    // We want to read the size of the Fifo memory
    m_fifo_size = get_register(IORegister::MemSize);

    // The reference driver checks some sizes here, but Qemu actually makes its buffers for
    // the fifo smaller then VMWare does, so they fail. The fifo still works, however, and the
    // numbers are accurate.

    // Check if the card is new enough and get the capabilities register
    if (m_device_version >= make_version_id(1)) {
        m_device_capabilities = get_register(IORegister::Capabilities);
    }

    // Check if interrupt masks are supported
    if (get_capability(Capabilities::IRQMask)) {
        // Get the card interrupt line
        m_interrupt_line = PCI::get_interrupt_line(address);
        change_irq_number(m_interrupt_line);
        // Mask all interrupts
        set_register(IORegister::IRQMask, 0xFF);
        // Clear all interrupts on the card
        IO::out32(m_io_base + static_cast<u32>(Ports::IRQStatus), 0xFF);
        // Enable the interrupt handler
        enable_irq();
    }

    dbgln("VMWareSVGA: handshake complete, version {}, max res: {}x{}, irq: {}", m_device_version & 0xFF, get_register(IORegister::MaxWidth), get_register(IORegister::MaxHeight), m_interrupt_line);

    // FIXME: Remove this when we get signals for updates from WindowServer
    RefPtr<Thread> update_thread;
    Process::create_kernel_process(update_thread, "VMWareSVGAThread", [&] {
        dbgln_if(VMWARESVGA_DEBUG, "VMWareSVGA: Thread is running");
        for (;;) {
            force_vblank();
            // We choose a time slightly smaller then 60Hz as
            // we will never be perfectly in sync with the monitor
            (void)Thread::current()->sleep(Time::from_milliseconds(15));
        }
    });
}

void VMWareSVGADevice::set_mode(u32 width, u32 height, u32 bpp)
{
    dbgln_if(VMWARESVGA_DEBUG, "VMWareSVGA: setting mode {}x{}x{}", width, height, bpp);
    // Set the mode itself
    set_register(IORegister::ConfigDone, false);
    set_register(IORegister::Enable, false);
    set_register(IORegister::Width, width);
    set_register(IORegister::Height, height);
    set_register(IORegister::BitsPerPixel, bpp);
    set_register(IORegister::Enable, true);
    // Fetch the video mode from the card
    // The resolution may not have been valid, so the card will choose one for us (most likely the highest / lowest possible resolution)
    m_framebuffer_width = get_register(IORegister::Width);
    m_framebuffer_height = get_register(IORegister::Height);
    m_framebuffer_bpp = get_register(IORegister::BitsPerPixel);
    m_framebuffer_pitch = get_register(IORegister::BytesPerLine);
    m_framebuffer_size = m_framebuffer_pitch * m_framebuffer_height;
    // Setup the fifo
    // Essentially we set FifoRegister::Min to the beginning of useable Fifo memory,
    // FifoRegister::Max to the maximum fifo size.
    // FifoRegister::NextCMD and FifoRegister::Stop are all set to FifoRegister::Min, as there is no data in the fifo yet.
    write_fifo(FifoRegister::Min, static_cast<u32>(FifoRegister::NumRegs) * sizeof(u32));
    write_fifo(FifoRegister::Max, m_fifo_size);
    write_fifo(FifoRegister::NextCMD, read_fifo(FifoRegister::Min));
    write_fifo(FifoRegister::Stop, read_fifo(FifoRegister::Min));

    // TODO: The reference driver "fakes" 3D support here. Should we do the same?
    set_register(IORegister::ConfigDone, true);

    // TODO: Test Interrupts
    dbgln("VMWareSVGA: Mode set was {}x{}x{}, pitch {}", width, height, bpp, m_framebuffer_pitch);

    m_mode_set = true;
}

int VMWareSVGADevice::ioctl(FileDescription&, unsigned request, FlatPtr arg)
{
    REQUIRE_PROMISE(video);
    switch (request) {
    case FB_IOCTL_GET_SIZE_IN_BYTES: {
        auto* out = (size_t*)arg;
        size_t value = m_framebuffer_size;
        if (!copy_to_user(out, &value)) {
            return -EFAULT;
        }
        return 0;
    }
    case FB_IOCTL_GET_BUFFER: {
        auto* index = (int*)arg;
        int value = 0;
        if (!copy_to_user(index, &value))
            return -EFAULT;
        return 0;
    }
    case FB_IOCTL_SET_BUFFER: {
        return -EINVAL;
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
        set_mode(resolution.width, resolution.height, 32);
        resolution.width = m_framebuffer_width;
        resolution.height = m_framebuffer_height;
        resolution.pitch = m_framebuffer_pitch;
        if (!copy_to_user(user_resolution, &resolution))
            return -EFAULT;
        return 0;
    }

    default: {
        return -EINVAL;
    }
    }
}

KResultOr<Region*> VMWareSVGADevice::mmap(Process& process, FileDescription&, const Range& range, u64 offset, int prot, bool shared)
{
    REQUIRE_PROMISE(video);
    if (!shared)
        return ENODEV;
    if (offset != 0)
        return ENXIO;
    if (range.size() != page_round_up(m_framebuffer_size))
        return EOVERFLOW;

    dbgln_if(VMWARESVGA_DEBUG, "VMWareSVGA: mmap; mmap addr:{} mmap size:{}", m_framebuffer_address_physical, m_framebuffer_size);

    auto vmobject = AnonymousVMObject::create_for_physical_range(m_framebuffer_address_physical, m_framebuffer_size);
    if (!vmobject)
        return ENOMEM;
    return process.space().allocate_region_with_vmobject(
        range,
        vmobject.release_nonnull(),
        0,
        "VMWare SVGA Framebuffer",
        prot,
        shared);
}

String VMWareSVGADevice::device_name() const
{
    return String::formatted("fb{}", minor());
}

void VMWareSVGADevice::handle_irq(const RegisterState&)
{
    // Read and clear the interrupts
    u16 irq_port = m_io_base + static_cast<u32>(Ports::IRQStatus);
    u32 irqs = IO::in32(irq_port);
    IO::out32(irq_port, irqs);

    if (!irqs) {
        dbgln("VMWareSVGA: received interrupt but no bit in IRQStatus is set - spurious or not for this device");
        return;
    }
}

u32* VMWareSVGADevice::reserve_fifo(size_t size)
{
    // TODO: support Fifo reserving
    VERIFY(m_fifo_bounce_buffer.size() == 0);
    m_fifo_bounce_buffer.ensure_capacity(size);
    m_fifo_bounce_buffer.resize(size, true);
    return m_fifo_bounce_buffer.data();
}

void VMWareSVGADevice::commit_fifo(size_t size)
{
    u32 next_cmd_in_fifo = read_fifo(FifoRegister::NextCMD); // Offset to the next command in the fifo
    u32 min_fifo = read_fifo(FifoRegister::Min);             // Offset to the beginning of usable fifo memory
    u32 max_fifo = read_fifo(FifoRegister::Max);             // Offset to the end of usable fifo memory

    // TODO: Support FifoCapability::Reserve
    // It would be faster, but Qemu doesnt support it (or at least doesnt report support for it),
    // so testing it is a bit on the hard side
    VERIFY(m_fifo_bounce_buffer.size() > 0);
    for (size_t i = 0; i < size; i++) {
        write_fifo(next_cmd_in_fifo / sizeof(u32), m_fifo_bounce_buffer[i]);
        next_cmd_in_fifo += sizeof(u32);
        if (next_cmd_in_fifo == max_fifo) {
            next_cmd_in_fifo = min_fifo;
        }
        write_fifo(FifoRegister::NextCMD, next_cmd_in_fifo);
    }
    m_fifo_bounce_buffer.clear_with_capacity();
}

void VMWareSVGADevice::force_vblank()
{
    update(Rect(0, 0, m_framebuffer_width, m_framebuffer_height));

    // Sync the FIFO buffer
    set_register(IORegister::Sync, 1);
    while (get_register(IORegister::Busy))
        ;
}

u32 VMWareSVGADevice::add_fence()
{
    TODO();
}

}
