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

NonnullOwnPtr<PIIX4HostBridge> PIIX4HostBridge::must_create_with_io_access()
{
    PCI::Domain domain { 0, 0, 0xff };
    return adopt_own_if_nonnull(new (nothrow) PIIX4HostBridge(domain)).release_nonnull();
}

PIIX4HostBridge::PIIX4HostBridge(PCI::Domain const& domain)
    : HostController(domain)
{
}

static u32 io_address_for_pci_field(BusNumber bus, DeviceNumber device, FunctionNumber function, u8 field)
{
    return 0x80000000u | (bus.value() << 16u) | (device.value() << 11u) | (function.value() << 8u) | (field & 0xfc);
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
