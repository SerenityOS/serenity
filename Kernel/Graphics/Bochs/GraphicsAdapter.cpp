/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/Checked.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Debug.h>
#include <Kernel/Graphics/Bochs/GraphicsAdapter.h>
#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/IO.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>

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
    PCI::ID id = PCI::get_id(address);
    VERIFY((id.vendor_id == PCI::VendorID::QEMUOld && id.device_id == 0x1111) || (id.vendor_id == PCI::VendorID::VirtualBox && id.device_id == 0xbeef));
    return adopt_ref(*new BochsGraphicsAdapter(address));
}

UNMAP_AFTER_INIT BochsGraphicsAdapter::BochsGraphicsAdapter(PCI::Address pci_address)
    : PCI::DeviceController(pci_address)
    , m_mmio_registers(PCI::get_BAR2(pci_address) & 0xfffffff0)
    , m_registers(Memory::map_typed_writable<BochsDisplayMMIORegisters volatile>(m_mmio_registers))
{
    // We assume safe resolutio is 1024x768x32
    m_framebuffer_console = Graphics::ContiguousFramebufferConsole::initialize(PhysicalAddress(PCI::get_BAR0(pci_address) & 0xfffffff0), 1024, 768, 1024 * sizeof(u32));
    // FIXME: This is a very wrong way to do this...
    GraphicsManagement::the().m_console = m_framebuffer_console;

    // Note: If we use VirtualBox graphics adapter (which is based on Bochs one), we need to use IO ports
    auto id = PCI::get_id(pci_address);
    if (id.vendor_id == 0x80ee && id.device_id == 0xbeef)
        m_io_required = true;

    // Note: According to Gerd Hoffmann - "The linux driver simply does
    // the unblank unconditionally. With bochs-display this is not needed but
    // it also has no bad side effect".
    unblank();
    set_safe_resolution();
}

UNMAP_AFTER_INIT void BochsGraphicsAdapter::initialize_framebuffer_devices()
{
    // FIXME: Find a better way to determine default resolution...
    m_framebuffer_device = FramebufferDevice::create(*this, 0, PhysicalAddress(PCI::get_BAR0(pci_address()) & 0xfffffff0), 1024, 768, 1024 * sizeof(u32));
    // FIXME: Would be nice to be able to return a KResult here.
    VERIFY(!m_framebuffer_device->initialize().is_error());
}

GraphicsDevice::Type BochsGraphicsAdapter::type() const
{
    if (PCI::get_class(pci_address()) == 0x3 && PCI::get_subclass(pci_address()) == 0x0)
        return Type::VGACompatible;
    return Type::Bochs;
}

void BochsGraphicsAdapter::unblank()
{
    full_memory_barrier();
    m_registers->vga_ioports[0] = 0x20;
    full_memory_barrier();
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
    m_registers->bochs_regs.enable = VBE_DISPI_DISABLED;
    full_memory_barrier();
    m_registers->bochs_regs.xres = width;
    m_registers->bochs_regs.yres = height;
    m_registers->bochs_regs.virt_width = width;
    m_registers->bochs_regs.virt_height = height * 2;
    m_registers->bochs_regs.bpp = 32;
    full_memory_barrier();
    m_registers->bochs_regs.enable = VBE_DISPI_ENABLED | VBE_DISPI_LFB_ENABLED;
    full_memory_barrier();
    m_registers->bochs_regs.bank = 0;
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
    if ((u16)width != m_registers->bochs_regs.xres || (u16)height != m_registers->bochs_regs.yres) {
        return false;
    }
    return true;
}

bool BochsGraphicsAdapter::set_y_offset(size_t output_port_index, size_t y_offset)
{
    VERIFY(output_port_index == 0);
    if (m_console_enabled)
        return false;
    m_registers->bochs_regs.y_offset = y_offset;
    return true;
}

void BochsGraphicsAdapter::enable_consoles()
{
    ScopedSpinLock lock(m_console_mode_switch_lock);
    VERIFY(m_framebuffer_console);
    m_console_enabled = true;
    m_registers->bochs_regs.y_offset = 0;
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
    m_registers->bochs_regs.y_offset = 0;
    m_framebuffer_console->disable();
    m_framebuffer_device->activate_writes();
}

}
