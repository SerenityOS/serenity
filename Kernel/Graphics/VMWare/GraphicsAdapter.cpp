/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/Checked.h>
#include <AK/Try.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Debug.h>
#include <Kernel/Graphics/Console/ContiguousFramebufferConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/VMWare/Definitions.h>
#include <Kernel/Graphics/VMWare/DisplayConnector.h>
#include <Kernel/Graphics/VMWare/GraphicsAdapter.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT RefPtr<VMWareGraphicsAdapter> VMWareGraphicsAdapter::try_initialize(PCI::DeviceIdentifier const& pci_device_identifier)
{
    PCI::HardwareID id = pci_device_identifier.hardware_id();
    VERIFY(id.vendor_id == PCI::VendorID::VMWare);
    // Note: We only support VMWare SVGA II adapter
    if (id.device_id != 0x0405)
        return {};
    auto adapter = MUST(adopt_nonnull_ref_or_enomem(new (nothrow) VMWareGraphicsAdapter(pci_device_identifier)));
    MUST(adapter->initialize_adapter());
    return adapter;
}

UNMAP_AFTER_INIT VMWareGraphicsAdapter::VMWareGraphicsAdapter(PCI::DeviceIdentifier const& pci_device_identifier)
    : PCI::Device(pci_device_identifier.address())
    , m_io_registers_base(PCI::get_BAR0(pci_device_identifier.address()) & 0xfffffff0)
{
    dbgln("VMWare SVGA @ {}, {}", pci_device_identifier.address(), m_io_registers_base);
}

u32 VMWareGraphicsAdapter::read_io_register(VMWareDisplayRegistersOffset register_offset) const
{
    SpinlockLocker locker(m_io_access_lock);
    m_io_registers_base.out<u32>(to_underlying(register_offset));
    return m_io_registers_base.offset(1).in<u32>();
}
void VMWareGraphicsAdapter::write_io_register(VMWareDisplayRegistersOffset register_offset, u32 value)
{
    SpinlockLocker locker(m_io_access_lock);
    m_io_registers_base.out<u32>(to_underlying(register_offset));
    m_io_registers_base.offset(1).out<u32>(value);
}

UNMAP_AFTER_INIT ErrorOr<void> VMWareGraphicsAdapter::negotiate_device_version()
{
    write_io_register(VMWareDisplayRegistersOffset::ID, vmware_svga_version_2_id);
    auto accepted_version = read_io_register(VMWareDisplayRegistersOffset::ID);
    dbgln("VMWare SVGA @ {}: Accepted version {}", pci_address(), accepted_version);
    if (read_io_register(VMWareDisplayRegistersOffset::ID) == vmware_svga_version_2_id)
        return {};
    return Error::from_errno(ENOTSUP);
}

UNMAP_AFTER_INIT ErrorOr<void> VMWareGraphicsAdapter::initialize_fifo_registers()
{
    auto framebuffer_size = read_io_register(VMWareDisplayRegistersOffset::FB_SIZE);
    auto fifo_size = read_io_register(VMWareDisplayRegistersOffset::MEM_SIZE);
    auto fifo_physical_address = PhysicalAddress(PCI::get_BAR2(pci_address()) & 0xfffffff0);

    dbgln("VMWare SVGA @ {}: framebuffer size {} bytes, FIFO size {} bytes @ {}", pci_address(), framebuffer_size, fifo_size, fifo_physical_address);
    if (framebuffer_size < 0x100000 || fifo_size < 0x10000) {
        dbgln("VMWare SVGA @ {}: invalid framebuffer or fifo size", pci_address());
        return Error::from_errno(ENOTSUP);
    }

    m_fifo_registers = TRY(Memory::map_typed<volatile VMWareDisplayFIFORegisters>(fifo_physical_address, fifo_size, Memory::Region::Access::ReadWrite));
    m_fifo_registers->start = 16;
    m_fifo_registers->size = 16 + (10 * 1024);
    m_fifo_registers->next_command = 16;
    m_fifo_registers->stop = 16;
    return {};
}

UNMAP_AFTER_INIT void VMWareGraphicsAdapter::print_svga_capabilities() const
{
    auto svga_capabilities = read_io_register(VMWareDisplayRegistersOffset::CAPABILITIES);
    dbgln("VMWare SVGA capabilities (raw {:x}):", svga_capabilities);
    if (svga_capabilities & (1 << 1))
        dbgln("\tRect copy");
    if (svga_capabilities & (1 << 5))
        dbgln("\tCursor");
    if (svga_capabilities & (1 << 6))
        dbgln("\tCursor Bypass");
    if (svga_capabilities & (1 << 7))
        dbgln("\tCursor Bypass 2");
    if (svga_capabilities & (1 << 8))
        dbgln("\t8 Bit emulation");
    if (svga_capabilities & (1 << 9))
        dbgln("\tAlpha Cursor");
    if (svga_capabilities & (1 << 14))
        dbgln("\t3D acceleration");
    if (svga_capabilities & (1 << 15))
        dbgln("\tExtended FIFO");
    if (svga_capabilities & (1 << 16))
        dbgln("\tMulti-monitor (legacy)");
    if (svga_capabilities & (1 << 17))
        dbgln("\tPitch lock");
    if (svga_capabilities & (1 << 18))
        dbgln("\tIRQ masking");
    if (svga_capabilities & (1 << 19))
        dbgln("\tDisplay topology");
    if (svga_capabilities & (1 << 20))
        dbgln("\tGMR");
    if (svga_capabilities & (1 << 21))
        dbgln("\tTraces");
    if (svga_capabilities & (1 << 22))
        dbgln("\tGMR2");
    if (svga_capabilities & (1 << 23))
        dbgln("\tScreen object 2");
}

ErrorOr<void> VMWareGraphicsAdapter::modeset_primary_screen_resolution(Badge<VMWareDisplayConnector>, size_t width, size_t height)
{
    auto max_width = read_io_register(VMWareDisplayRegistersOffset::MAX_WIDTH);
    auto max_height = read_io_register(VMWareDisplayRegistersOffset::MAX_HEIGHT);
    if (width > max_width || height > max_height)
        return Error::from_errno(ENOTSUP);
    modeset_primary_screen_resolution(width, height);
    return {};
}

size_t VMWareGraphicsAdapter::primary_screen_width(Badge<VMWareDisplayConnector>) const
{
    SpinlockLocker locker(m_operation_lock);
    return read_io_register(VMWareDisplayRegistersOffset::WIDTH);
}
size_t VMWareGraphicsAdapter::primary_screen_height(Badge<VMWareDisplayConnector>) const
{
    SpinlockLocker locker(m_operation_lock);
    return read_io_register(VMWareDisplayRegistersOffset::HEIGHT);
}
size_t VMWareGraphicsAdapter::primary_screen_pitch(Badge<VMWareDisplayConnector>) const
{
    SpinlockLocker locker(m_operation_lock);
    return read_io_register(VMWareDisplayRegistersOffset::BYTES_PER_LINE);
}

void VMWareGraphicsAdapter::primary_screen_flush(Badge<VMWareDisplayConnector>, size_t current_width, size_t current_height)
{
    SpinlockLocker locker(m_operation_lock);
    m_fifo_registers->start = 16;
    m_fifo_registers->size = 16 + (10 * 1024);
    m_fifo_registers->next_command = 16 + 4 * 5;
    m_fifo_registers->stop = 16;
    m_fifo_registers->commands[0] = 1;
    m_fifo_registers->commands[1] = 0;
    m_fifo_registers->commands[2] = 0;
    m_fifo_registers->commands[3] = current_width;
    m_fifo_registers->commands[4] = current_height;
    write_io_register(VMWareDisplayRegistersOffset::SYNC, 1);
}

void VMWareGraphicsAdapter::modeset_primary_screen_resolution(size_t width, size_t height)
{
    SpinlockLocker locker(m_operation_lock);
    write_io_register(VMWareDisplayRegistersOffset::ENABLE, 0);
    write_io_register(VMWareDisplayRegistersOffset::WIDTH, width);
    write_io_register(VMWareDisplayRegistersOffset::HEIGHT, height);
    write_io_register(VMWareDisplayRegistersOffset::BITS_PER_PIXEL, 32);
    write_io_register(VMWareDisplayRegistersOffset::ENABLE, 1);
    write_io_register(VMWareDisplayRegistersOffset::CONFIG_DONE, 1);
}

UNMAP_AFTER_INIT ErrorOr<void> VMWareGraphicsAdapter::initialize_adapter()
{
    TRY(negotiate_device_version());
    print_svga_capabilities();
    TRY(initialize_fifo_registers());
    // Note: enable the device by modesetting the primary screen resolution
    modeset_primary_screen_resolution(640, 480);

    m_display_connector = VMWareDisplayConnector::must_create(*this, PhysicalAddress(PCI::get_BAR1(pci_address()) & 0xfffffff0));
    TRY(m_display_connector->set_safe_mode_setting());
    return {};
}

bool VMWareGraphicsAdapter::vga_compatible() const
{
    return false;
}

}
