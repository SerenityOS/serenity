/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/Checked.h>
#include <AK/Singleton.h>
#include <Kernel/Debug.h>
#include <Kernel/Graphics/Bochs.h>
#include <Kernel/Graphics/BochsGraphicsAdapter.h>
#include <Kernel/Graphics/Console/FramebufferConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/IO.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/Process.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/TypedMapping.h>
#include <LibC/errno_numbers.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

struct [[gnu::packed]] DISPIInterface {
    u16 index_id;
    u16 xres;
    u16 yres;
    u16 bpp;
    u16 enable;
    u16 bank;
    u16 virt_width;
    u16 virt_height;
    u16 x_offset;
    u16 y_offset;
};

struct [[gnu::packed]] BochsDisplayMMIORegisters {
    u8 edid_data[0x400];
    u16 vga_ioports[0x10];
    u8 reserved[0xE0];
    DISPIInterface bochs_regs;
};

UNMAP_AFTER_INIT NonnullRefPtr<BochsGraphicsAdapter> BochsGraphicsAdapter::initialize(PCI::Address address)
{
    return adopt_ref(*new BochsGraphicsAdapter(address));
}

UNMAP_AFTER_INIT BochsGraphicsAdapter::BochsGraphicsAdapter(PCI::Address pci_address)
    : PCI::DeviceController(pci_address)
    , m_mmio_registers(PCI::get_BAR2(pci_address) & 0xfffffff0)
{
    // We assume safe resolutio is 1024x768x32
    m_framebuffer_console = Graphics::FramebufferConsole::initialize(PhysicalAddress(PCI::get_BAR0(pci_address) & 0xfffffff0), 1024, 768, 1024 * sizeof(u32));
    // FIXME: This is a very wrong way to do this...
    GraphicsManagement::the().m_console = m_framebuffer_console;

    // Note: If we use VirtualBox graphics adapter (which is based on Bochs one), we need to use IO ports
    auto id = PCI::get_id(pci_address);
    if (id.vendor_id == 0x80ee && id.device_id == 0xbeef)
        m_io_required = true;
    set_safe_resolution();
}

UNMAP_AFTER_INIT void BochsGraphicsAdapter::initialize_framebuffer_devices()
{
    // FIXME: Find a better way to determine default resolution...
    m_framebuffer_device = FramebufferDevice::create(*this, 0, PhysicalAddress(PCI::get_BAR0(pci_address()) & 0xfffffff0), 1024, 768, 1024 * sizeof(u32));
    m_framebuffer_device->initialize();
}

GraphicsDevice::Type BochsGraphicsAdapter::type() const
{
    if (PCI::get_class(pci_address()) == 0x3 && PCI::get_subclass(pci_address()) == 0x0)
        return Type::VGACompatible;
    return Type::Bochs;
}

void BochsGraphicsAdapter::set_safe_resolution()
{
    VERIFY(m_framebuffer_console);
    auto result = try_to_set_resolution(0, 1024, 768);
    VERIFY(result);
}

static void set_register_with_io(u16 index, u16 data)
{
    IO::out16(VBE_DISPI_IOPORT_INDEX, index);
    IO::out16(VBE_DISPI_IOPORT_DATA, data);
}

static u16 get_register_with_io(u16 index)
{
    IO::out16(VBE_DISPI_IOPORT_INDEX, index);
    return IO::in16(VBE_DISPI_IOPORT_DATA);
}

void BochsGraphicsAdapter::set_resolution_registers_via_io(size_t width, size_t height)
{
    dbgln_if(BXVGA_DEBUG, "BochsGraphicsAdapter resolution registers set to - {}x{}", width, height);

    set_register_with_io(VBE_DISPI_INDEX_ENABLE, VBE_DISPI_DISABLED);
    set_register_with_io(VBE_DISPI_INDEX_XRES, (u16)width);
    set_register_with_io(VBE_DISPI_INDEX_YRES, (u16)height);
    set_register_with_io(VBE_DISPI_INDEX_VIRT_WIDTH, (u16)width);
    set_register_with_io(VBE_DISPI_INDEX_VIRT_HEIGHT, (u16)height * 2);
    set_register_with_io(VBE_DISPI_INDEX_BPP, 32);
    set_register_with_io(VBE_DISPI_INDEX_ENABLE, VBE_DISPI_ENABLED | VBE_DISPI_LFB_ENABLED);
    set_register_with_io(VBE_DISPI_INDEX_BANK, 0);
}

void BochsGraphicsAdapter::set_resolution_registers(size_t width, size_t height)
{
    dbgln_if(BXVGA_DEBUG, "BochsGraphicsAdapter resolution registers set to - {}x{}", width, height);
    auto registers = map_typed_writable<volatile BochsDisplayMMIORegisters>(m_mmio_registers);
    registers->bochs_regs.enable = VBE_DISPI_DISABLED;
    full_memory_barrier();
    registers->bochs_regs.xres = width;
    registers->bochs_regs.yres = height;
    registers->bochs_regs.virt_width = width;
    registers->bochs_regs.virt_height = height * 2;
    registers->bochs_regs.bpp = 32;
    full_memory_barrier();
    registers->bochs_regs.enable = VBE_DISPI_ENABLED | VBE_DISPI_LFB_ENABLED;
    full_memory_barrier();
    registers->bochs_regs.bank = 0;
}

bool BochsGraphicsAdapter::try_to_set_resolution(size_t output_port_index, size_t width, size_t height)
{
    // Note: There's only one output port for this adapter
    VERIFY(output_port_index == 0);
    VERIFY(m_framebuffer_console);
    if (Checked<size_t>::multiplication_would_overflow(width, height, sizeof(u32)))
        return false;

    if (m_io_required)
        set_resolution_registers_via_io(width, height);
    else
        set_resolution_registers(width, height);
    dbgln_if(BXVGA_DEBUG, "BochsGraphicsAdapter resolution test - {}x{}", width, height);
    if (m_io_required) {
        if (!validate_setup_resolution_with_io(width, height))
            return false;
    } else {
        if (!validate_setup_resolution(width, height))
            return false;
    }

    dbgln("BochsGraphicsAdapter: resolution set to {}x{}", width, height);
    m_framebuffer_console->set_resolution(width, height, width * sizeof(u32));
    return true;
}

bool BochsGraphicsAdapter::validate_setup_resolution_with_io(size_t width, size_t height)
{
    if ((u16)width != get_register_with_io(VBE_DISPI_INDEX_XRES) || (u16)height != get_register_with_io(VBE_DISPI_INDEX_YRES)) {
        return false;
    }
    return true;
}

bool BochsGraphicsAdapter::validate_setup_resolution(size_t width, size_t height)
{
    auto registers = map_typed_writable<volatile BochsDisplayMMIORegisters>(m_mmio_registers);
    if ((u16)width != registers->bochs_regs.xres || (u16)height != registers->bochs_regs.yres) {
        return false;
    }
    return true;
}

bool BochsGraphicsAdapter::set_y_offset(size_t output_port_index, size_t y_offset)
{
    VERIFY(output_port_index == 0);
    if (m_console_enabled)
        return false;
    auto registers = map_typed_writable<volatile BochsDisplayMMIORegisters>(m_mmio_registers);
    registers->bochs_regs.y_offset = y_offset;
    return true;
}

void BochsGraphicsAdapter::enable_consoles()
{
    ScopedSpinLock lock(m_console_mode_switch_lock);
    VERIFY(m_framebuffer_console);
    m_console_enabled = true;
    auto registers = map_typed_writable<volatile BochsDisplayMMIORegisters>(m_mmio_registers);
    registers->bochs_regs.y_offset = 0;
    if (m_framebuffer_device)
        m_framebuffer_device->deactivate_writes();
    m_framebuffer_console->enable();
}
void BochsGraphicsAdapter::disable_consoles()
{
    ScopedSpinLock lock(m_console_mode_switch_lock);
    VERIFY(m_framebuffer_console);
    VERIFY(m_framebuffer_device);
    m_console_enabled = false;
    auto registers = map_typed_writable<volatile BochsDisplayMMIORegisters>(m_mmio_registers);
    registers->bochs_regs.y_offset = 0;
    m_framebuffer_console->disable();
    m_framebuffer_device->activate_writes();
}

}
