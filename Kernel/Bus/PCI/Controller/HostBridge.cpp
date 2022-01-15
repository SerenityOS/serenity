/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Controller/HostBridge.h>
#include <Kernel/Sections.h>

namespace Kernel::PCI {

NonnullOwnPtr<HostBridge> HostBridge::must_create_with_io_access()
{
    PCI::Domain domain { 0, 0, 0xff };
    return adopt_own_if_nonnull(new (nothrow) HostBridge(domain)).release_nonnull();
}

HostBridge::HostBridge(PCI::Domain const& domain)
    : HostController(domain)
    , m_enumerated_buses(256, false)
{
}

UNMAP_AFTER_INIT Optional<u8> HostBridge::get_capabilities_pointer_for_function(BusNumber bus, DeviceNumber device, FunctionNumber function)
{
    if (read16_field(bus, device, function, PCI::RegisterOffset::STATUS) & (1 << 4)) {
        return read8_field(bus, device, function, PCI::RegisterOffset::CAPABILITIES_POINTER);
    }
    return {};
}

UNMAP_AFTER_INIT Vector<Capability> HostBridge::get_capabilities_for_function(BusNumber bus, DeviceNumber device, FunctionNumber function)
{
    auto capabilities_pointer = get_capabilities_pointer_for_function(bus, device, function);
    if (!capabilities_pointer.has_value()) {
        return {};
    }
    Vector<Capability> capabilities;
    auto capability_pointer = capabilities_pointer.value();
    while (capability_pointer != 0) {
        u16 capability_header = read16_field(bus, device, function, capability_pointer);
        u8 capability_id = capability_header & 0xff;

        // FIXME: Don't attach a PCI address to a capability object
        capabilities.append({ Address(domain_number(), bus.value(), device.value(), function.value()), capability_id, capability_pointer });
        capability_pointer = capability_header >> 8;
    }
    return capabilities;
}

UNMAP_AFTER_INIT void HostBridge::enumerate_functions(Function<void(DeviceIdentifier)> const& callback, BusNumber bus, DeviceNumber device, FunctionNumber function, bool recursive_search_into_bridges)
{
    dbgln_if(PCI_DEBUG, "PCI: Enumerating function, bus={}, device={}, function={}", bus, device, function);
    Address address(domain_number(), bus.value(), device.value(), function.value());
    auto pci_class = (read8_field(bus, device, function, PCI::RegisterOffset::CLASS) << 8u) | read8_field(bus, device, function, PCI::RegisterOffset::SUBCLASS);

    HardwareID id = { read16_field(bus, device, function, PCI::RegisterOffset::VENDOR_ID), read16_field(bus, device, function, PCI::RegisterOffset::DEVICE_ID) };
    ClassCode class_code = read8_field(bus, device, function, PCI::RegisterOffset::CLASS);
    SubclassCode subclass_code = read8_field(bus, device, function, PCI::RegisterOffset::SUBCLASS);
    ProgrammingInterface prog_if = read8_field(bus, device, function, PCI::RegisterOffset::PROG_IF);
    RevisionID revision_id = read8_field(bus, device, function, PCI::RegisterOffset::REVISION_ID);
    SubsystemID subsystem_id = read16_field(bus, device, function, PCI::RegisterOffset::SUBSYSTEM_ID);
    SubsystemVendorID subsystem_vendor_id = read16_field(bus, device, function, PCI::RegisterOffset::SUBSYSTEM_VENDOR_ID);
    InterruptLine interrupt_line = read8_field(bus, device, function, PCI::RegisterOffset::INTERRUPT_LINE);
    InterruptPin interrupt_pin = read8_field(bus, device, function, PCI::RegisterOffset::INTERRUPT_PIN);
    auto capabilities = get_capabilities_for_function(bus, device, function);
    callback(DeviceIdentifier { address, id, revision_id, class_code, subclass_code, prog_if, subsystem_id, subsystem_vendor_id, interrupt_line, interrupt_pin, capabilities });

    if (pci_class == (to_underlying(PCI::ClassID::Bridge) << 8 | to_underlying(PCI::Bridge::SubclassID::PCI_TO_PCI))
        && recursive_search_into_bridges
        && (!m_enumerated_buses.get(read8_field(bus, device, function, PCI::RegisterOffset::SECONDARY_BUS)))) {
        u8 secondary_bus = read8_field(bus, device, function, PCI::RegisterOffset::SECONDARY_BUS);
        dbgln_if(PCI_DEBUG, "PCI: Found secondary bus: {}", secondary_bus);
        VERIFY(secondary_bus != bus);
        m_enumerated_buses.set(secondary_bus, true);
        enumerate_bus(callback, secondary_bus, recursive_search_into_bridges);
    }
}

UNMAP_AFTER_INIT void HostBridge::enumerate_device(Function<void(DeviceIdentifier)> const& callback, BusNumber bus, DeviceNumber device, bool recursive_search_into_bridges)
{
    dbgln_if(PCI_DEBUG, "PCI: Enumerating device in bus={}, device={}", bus, device);
    if (read16_field(bus, device, 0, PCI::RegisterOffset::VENDOR_ID) == PCI::none_value)
        return;
    enumerate_functions(callback, bus, device, 0, recursive_search_into_bridges);
    if (!(read8_field(bus, device, 0, PCI::RegisterOffset::HEADER_TYPE) & 0x80))
        return;
    for (u8 function = 1; function < 8; ++function) {
        if (read16_field(bus, device, function, PCI::RegisterOffset::VENDOR_ID) != PCI::none_value)
            enumerate_functions(callback, bus, device, function, recursive_search_into_bridges);
    }
}

UNMAP_AFTER_INIT void HostBridge::enumerate_bus(Function<void(DeviceIdentifier)> const& callback, BusNumber bus, bool recursive_search_into_bridges)
{
    dbgln_if(PCI_DEBUG, "PCI: Enumerating bus {}", bus);
    for (u8 device = 0; device < 32; ++device)
        enumerate_device(callback, bus, device, recursive_search_into_bridges);
}

UNMAP_AFTER_INIT void HostBridge::enumerate_attached_devices(Function<void(DeviceIdentifier)> callback)
{
    VERIFY(Access::the().access_lock().is_locked());
    VERIFY(Access::the().scan_lock().is_locked());
    // First scan bus 0. Find any device on that bus, and if it's a PCI-to-PCI
    // bridge, recursively scan it too.
    m_enumerated_buses.set(m_domain.start_bus(), true);
    enumerate_bus(callback, m_domain.start_bus(), true);

    // Handle Multiple PCI host bridges on bus 0, device 0, functions 1-7 (function 0
    // is the main host bridge).
    // If we happen to miss some PCI buses because they are not reachable through
    // recursive PCI-to-PCI bridges starting from bus 0, we might find them here.
    if ((read8_field(0, 0, 0, PCI::RegisterOffset::HEADER_TYPE) & 0x80) != 0) {
        for (int bus_as_function_number = 1; bus_as_function_number < 8; ++bus_as_function_number) {
            if (read16_field(0, 0, bus_as_function_number, PCI::RegisterOffset::VENDOR_ID) == PCI::none_value)
                continue;
            if (read16_field(0, 0, bus_as_function_number, PCI::RegisterOffset::CLASS) != 0x6)
                continue;
            if (Checked<u8>::addition_would_overflow(m_domain.start_bus(), bus_as_function_number))
                break;
            if (m_enumerated_buses.get(m_domain.start_bus() + bus_as_function_number))
                continue;
            enumerate_bus(callback, m_domain.start_bus() + bus_as_function_number, false);
            m_enumerated_buses.set(m_domain.start_bus() + bus_as_function_number, true);
        }
    }
}

static u32 io_address_for_pci_field(BusNumber bus, DeviceNumber device, FunctionNumber function, u8 field)
{
    return 0x80000000u | (bus.value() << 16u) | (device.value() << 11u) | (function.value() << 8u) | (field & 0xfc);
}

void HostBridge::write8_field(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field, u8 value)
{
    IO::out32(PCI::address_port, io_address_for_pci_field(bus, device, function, field));
    IO::out8(PCI::value_port + (field & 3), value);
}
void HostBridge::write16_field(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field, u16 value)
{
    IO::out32(PCI::address_port, io_address_for_pci_field(bus, device, function, field));
    IO::out16(PCI::value_port + (field & 2), value);
}
void HostBridge::write32_field(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field, u32 value)
{
    IO::out32(PCI::address_port, io_address_for_pci_field(bus, device, function, field));
    IO::out32(PCI::value_port, value);
}

u8 HostBridge::read8_field(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field)
{
    IO::out32(PCI::address_port, io_address_for_pci_field(bus, device, function, field));
    return IO::in8(PCI::value_port + (field & 3));
}
u16 HostBridge::read16_field(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field)
{
    IO::out32(PCI::address_port, io_address_for_pci_field(bus, device, function, field));
    return IO::in16(PCI::value_port + (field & 2));
}
u32 HostBridge::read32_field(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field)
{
    IO::out32(PCI::address_port, io_address_for_pci_field(bus, device, function, field));
    return IO::in32(PCI::value_port);
}

u8 HostBridge::read8_field(BusNumber bus, DeviceNumber device, FunctionNumber function, PCI::RegisterOffset field)
{
    return read8_field(bus, device, function, to_underlying(field));
}
u16 HostBridge::read16_field(BusNumber bus, DeviceNumber device, FunctionNumber function, PCI::RegisterOffset field)
{
    return read16_field(bus, device, function, to_underlying(field));
}

}
