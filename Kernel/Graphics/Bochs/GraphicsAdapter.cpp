/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/Checked.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Debug.h>
#include <Kernel/Graphics/Bochs/GraphicsAdapter.h>
#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>

#define VBE_DISPI_IOPORT_INDEX 0x01CE
#define VBE_DISPI_IOPORT_DATA 0x01CF

#define VBE_DISPI_ID5 0xB0C5

#define BOCHS_DISPLAY_LITTLE_ENDIAN 0x1e1e1e1e
#define BOCHS_DISPLAY_BIG_ENDIAN 0xbebebebe

namespace Kernel {

enum class BochsFramebufferSettings {
    Enabled = 0x1,
    LinearFramebuffer = 0x40,
};

enum class BochsDISPIRegisters {
    ID = 0x0,
    XRES = 0x1,
    YRES = 0x2,
    BPP = 0x3,
    ENABLE = 0x4,
    BANK = 0x5,
    VIRT_WIDTH = 0x6,
    VIRT_HEIGHT = 0x7,
    X_OFFSET = 0x8,
    Y_OFFSET = 0x9,
};

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

struct [[gnu::packed]] ExtensionRegisters {
    u32 region_size;
    u32 framebuffer_byteorder;
};

struct [[gnu::packed]] BochsDisplayMMIORegisters {
    u8 edid_data[0x400];
    u16 vga_ioports[0x10];
    u8 reserved[0xE0];
    DISPIInterface bochs_regs;
    u8 reserved2[0x100 - sizeof(DISPIInterface)];
    ExtensionRegisters extension_regs;
};

UNMAP_AFTER_INIT NonnullRefPtr<BochsGraphicsAdapter> BochsGraphicsAdapter::initialize(PCI::DeviceIdentifier const& pci_device_identifier)
{
    PCI::HardwareID id = pci_device_identifier.hardware_id();
    VERIFY((id.vendor_id == PCI::VendorID::QEMUOld && id.device_id == 0x1111) || (id.vendor_id == PCI::VendorID::VirtualBox && id.device_id == 0xbeef));
    return adopt_ref(*new BochsGraphicsAdapter(pci_device_identifier));
}

void BochsGraphicsAdapter::set_framebuffer_to_big_endian_format()
{
    dbgln_if(BXVGA_DEBUG, "BochsGraphicsAdapter set_framebuffer_to_big_endian_format");
    full_memory_barrier();
    if (m_registers->extension_regs.region_size == 0xFFFFFFFF || m_registers->extension_regs.region_size == 0)
        return;
    full_memory_barrier();
    m_registers->extension_regs.framebuffer_byteorder = BOCHS_DISPLAY_BIG_ENDIAN;
    full_memory_barrier();
}

void BochsGraphicsAdapter::set_framebuffer_to_little_endian_format()
{
    dbgln_if(BXVGA_DEBUG, "BochsGraphicsAdapter set_framebuffer_to_little_endian_format");
    full_memory_barrier();
    if (m_registers->extension_regs.region_size == 0xFFFFFFFF || m_registers->extension_regs.region_size == 0)
        return;
    full_memory_barrier();
    m_registers->extension_regs.framebuffer_byteorder = BOCHS_DISPLAY_LITTLE_ENDIAN;
    full_memory_barrier();
}

UNMAP_AFTER_INIT BochsGraphicsAdapter::BochsGraphicsAdapter(PCI::DeviceIdentifier const& pci_device_identifier)
    : PCI::Device(pci_device_identifier.address())
    , m_mmio_registers(PCI::get_BAR2(pci_device_identifier.address()) & 0xfffffff0)
    , m_registers(Memory::map_typed_writable<BochsDisplayMMIORegisters volatile>(m_mmio_registers).release_value_but_fixme_should_propagate_errors())
{
    // We assume safe resolution is 1024x768x32
    m_framebuffer_console = Graphics::ContiguousFramebufferConsole::initialize(PhysicalAddress(PCI::get_BAR0(pci_device_identifier.address()) & 0xfffffff0), 1024, 768, 1024 * sizeof(u32));
    GraphicsManagement::the().set_console(*m_framebuffer_console);

    auto vendor_id = pci_device_identifier.hardware_id().vendor_id;
    auto device_id = pci_device_identifier.hardware_id().device_id;
    auto revision_id = pci_device_identifier.revision_id();

    auto is_bochs = vendor_id == PCI::VendorID::QEMUOld && device_id == 0x1111 && revision_id == 0;
    auto is_virtualbox = vendor_id == PCI::VendorID::VirtualBox && device_id == 0xbeef;

    if (is_bochs || is_virtualbox)
        m_io_required = true;

    if (pci_device_identifier.class_code().value() == 0x3 && pci_device_identifier.subclass_code().value() == 0x0)
        m_is_vga_capable = true;

    // Note: According to Gerd Hoffmann - "The linux driver simply does
    // the unblank unconditionally. With bochs-display this is not needed but
    // it also has no bad side effect".
    unblank();
    set_safe_resolution();
}

UNMAP_AFTER_INIT void BochsGraphicsAdapter::initialize_framebuffer_devices()
{
    // FIXME: Find a better way to determine default resolution...
    m_framebuffer_device = FramebufferDevice::create(*this, PhysicalAddress(PCI::get_BAR0(pci_address()) & 0xfffffff0), 1024, 768, 1024 * sizeof(u32));
    // While write-combine helps greatly on actual hardware, it greatly reduces performance in QEMU
    m_framebuffer_device->enable_write_combine(false);
    // FIXME: Would be nice to be able to return a ErrorOr<void> here.
    VERIFY(!m_framebuffer_device->try_to_initialize().is_error());
}

bool BochsGraphicsAdapter::vga_compatible() const
{
    return m_is_vga_capable;
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

BochsGraphicsAdapter::IndexID BochsGraphicsAdapter::index_id() const
{
    if (m_io_required) {
        return get_register_with_io(0);
    }
    return m_registers->bochs_regs.index_id;
}

void BochsGraphicsAdapter::set_resolution_registers_via_io(size_t width, size_t height)
{
    dbgln_if(BXVGA_DEBUG, "BochsGraphicsAdapter resolution registers set to - {}x{}", width, height);

    set_register_with_io(to_underlying(BochsDISPIRegisters::ENABLE), 0);
    set_register_with_io(to_underlying(BochsDISPIRegisters::XRES), (u16)width);
    set_register_with_io(to_underlying(BochsDISPIRegisters::YRES), (u16)height);
    set_register_with_io(to_underlying(BochsDISPIRegisters::VIRT_WIDTH), (u16)width);
    set_register_with_io(to_underlying(BochsDISPIRegisters::VIRT_HEIGHT), (u16)height * 2);
    set_register_with_io(to_underlying(BochsDISPIRegisters::BPP), 32);
    set_register_with_io(to_underlying(BochsDISPIRegisters::ENABLE), to_underlying(BochsFramebufferSettings::Enabled) | to_underlying(BochsFramebufferSettings::LinearFramebuffer));
    set_register_with_io(to_underlying(BochsDISPIRegisters::BANK), 0);
}

void BochsGraphicsAdapter::set_resolution_registers(size_t width, size_t height)
{
    dbgln_if(BXVGA_DEBUG, "BochsGraphicsAdapter resolution registers set to - {}x{}", width, height);
    m_registers->bochs_regs.enable = 0;
    full_memory_barrier();
    m_registers->bochs_regs.xres = width;
    m_registers->bochs_regs.yres = height;
    m_registers->bochs_regs.virt_width = width;
    m_registers->bochs_regs.virt_height = height * 2;
    m_registers->bochs_regs.bpp = 32;
    full_memory_barrier();
    m_registers->bochs_regs.enable = to_underlying(BochsFramebufferSettings::Enabled) | to_underlying(BochsFramebufferSettings::LinearFramebuffer);
    full_memory_barrier();
    m_registers->bochs_regs.bank = 0;
    if (index_id().value() == VBE_DISPI_ID5) {
        set_framebuffer_to_little_endian_format();
    }
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
    if ((u16)width != get_register_with_io(to_underlying(BochsDISPIRegisters::XRES)) || (u16)height != get_register_with_io(to_underlying(BochsDISPIRegisters::YRES))) {
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
    SpinlockLocker lock(m_console_mode_switch_lock);
    VERIFY(m_framebuffer_console);
    m_console_enabled = true;
    m_registers->bochs_regs.y_offset = 0;
    if (m_framebuffer_device)
        m_framebuffer_device->deactivate_writes();
    m_framebuffer_console->enable();
}
void BochsGraphicsAdapter::disable_consoles()
{
    SpinlockLocker lock(m_console_mode_switch_lock);
    VERIFY(m_framebuffer_console);
    VERIFY(m_framebuffer_device);
    m_console_enabled = false;
    m_registers->bochs_regs.y_offset = 0;
    m_framebuffer_console->disable();
    m_framebuffer_device->activate_writes();
}

ErrorOr<ByteBuffer> BochsGraphicsAdapter::get_edid(size_t output_port_index) const
{
    if (output_port_index != 0)
        return Error::from_errno(ENODEV);

    return ByteBuffer::copy(const_cast<u8 const*>(m_registers->edid_data), sizeof(m_registers->edid_data));
}

}
