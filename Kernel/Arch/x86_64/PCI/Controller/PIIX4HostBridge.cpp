/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86_64/IO.h>
#include <Kernel/Arch/x86_64/PCI/Controller/PIIX4HostBridge.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Sections.h>

namespace Kernel::PCI {

static u32 io_address_for_pci_field(BusNumber bus, DeviceNumber device, FunctionNumber function, u8 field)
{
    return 0x80000000u | (bus.value() << 16u) | (device.value() << 11u) | (function.value() << 8u) | (field & 0xfc);
}

ErrorOr<NonnullRefPtr<PIIX4HostBridge>> PIIX4HostBridge::create()
{
    PCI::Domain domain { 0, 0, 0xff };
    Address address(domain.domain_number(), 0, 0, 0);

    auto get_u16_field = [](PCI::RegisterOffset offset) -> u16 {
        IO::out32(PCI::address_port, io_address_for_pci_field(0, 0, 0, to_underlying(offset)));
        return IO::in16(PCI::value_port + (to_underlying(offset) & 2));
    };

    auto get_u8_field = [](PCI::RegisterOffset offset) -> u16 {
        IO::out32(PCI::address_port, io_address_for_pci_field(0, 0, 0, to_underlying(offset)));
        return IO::in8(PCI::value_port + (to_underlying(offset) & 3));
    };

    HardwareID id = { get_u16_field(PCI::RegisterOffset::VENDOR_ID), get_u16_field(PCI::RegisterOffset::DEVICE_ID) };
    ClassCode class_code = get_u8_field(PCI::RegisterOffset::CLASS);
    SubclassCode subclass_code = get_u8_field(PCI::RegisterOffset::SUBCLASS);
    ProgrammingInterface prog_if = get_u8_field(PCI::RegisterOffset::PROG_IF);
    RevisionID revision_id = get_u8_field(PCI::RegisterOffset::REVISION_ID);
    SubsystemID subsystem_id = get_u16_field(PCI::RegisterOffset::SUBSYSTEM_ID);
    SubsystemVendorID subsystem_vendor_id = get_u16_field(PCI::RegisterOffset::SUBSYSTEM_VENDOR_ID);
    InterruptLine interrupt_line = get_u8_field(PCI::RegisterOffset::INTERRUPT_LINE);
    InterruptPin interrupt_pin = get_u8_field(PCI::RegisterOffset::INTERRUPT_PIN);
    auto identifier = EnumerableDeviceIdentifier { address, id, revision_id, class_code, subclass_code, prog_if, subsystem_id, subsystem_vendor_id, interrupt_line, interrupt_pin };
    auto host_bridge_device = TRY(Device::from_enumerable_identifier(identifier));

    dbgln("PCI/x86: PIIX4-compatible host bridge: {:04X}:{:02X}:{:02X}:{:02X}: vendor, dev_id {:04x}:{:04x}",
        (u32)domain.domain_number(),
        (u8)domain.start_bus(),
        (u8)0,
        (u8)0,
        (u16)id.vendor_id,
        (u16)id.device_id);

    auto bus = TRY(Bus::create(0, host_bridge_device, nullptr));
    auto bridge = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) PIIX4HostBridge(domain, bus)));
    host_bridge_device->set_host_controller(*bridge);

    {
        SpinlockLocker locker(bridge->m_access_lock);
        TRY(bridge->enumerate_capabilities_for_function(bus, host_bridge_device));
    }

    return bridge;
}

PIIX4HostBridge::PIIX4HostBridge(PCI::Domain const& domain, NonnullRefPtr<Bus> root_bus)
    : HostController(domain, root_bus)
{
}

void PIIX4HostBridge::write8_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field, u8 value)
{
    VERIFY(m_access_lock.is_locked());
    IO::out32(PCI::address_port, io_address_for_pci_field(bus, device, function, field));
    IO::out8(PCI::value_port + (field & 3), value);
}

void PIIX4HostBridge::write16_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field, u16 value)
{
    VERIFY(m_access_lock.is_locked());
    IO::out32(PCI::address_port, io_address_for_pci_field(bus, device, function, field));
    IO::out16(PCI::value_port + (field & 2), value);
}

void PIIX4HostBridge::write32_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field, u32 value)
{
    VERIFY(m_access_lock.is_locked());
    IO::out32(PCI::address_port, io_address_for_pci_field(bus, device, function, field));
    IO::out32(PCI::value_port, value);
}

u8 PIIX4HostBridge::read8_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field)
{
    VERIFY(m_access_lock.is_locked());
    IO::out32(PCI::address_port, io_address_for_pci_field(bus, device, function, field));
    return IO::in8(PCI::value_port + (field & 3));
}

u16 PIIX4HostBridge::read16_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field)
{
    VERIFY(m_access_lock.is_locked());
    IO::out32(PCI::address_port, io_address_for_pci_field(bus, device, function, field));
    return IO::in16(PCI::value_port + (field & 2));
}

u32 PIIX4HostBridge::read32_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field)
{
    VERIFY(m_access_lock.is_locked());
    IO::out32(PCI::address_port, io_address_for_pci_field(bus, device, function, field));
    return IO::in32(PCI::value_port);
}

}
