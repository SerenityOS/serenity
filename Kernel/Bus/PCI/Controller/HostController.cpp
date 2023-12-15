/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Controller/HostController.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Sections.h>

namespace Kernel::PCI {

HostController::HostController(PCI::Domain const& domain)
    : m_domain(domain)
    , m_enumerated_buses(Bitmap::create(256, false).release_value_but_fixme_should_propagate_errors())
{
}

void HostController::write8_field(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field, u8 value)
{
    SpinlockLocker locker(m_access_lock);
    write8_field_locked(bus, device, function, field, value);
}

void HostController::write16_field(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field, u16 value)
{
    SpinlockLocker locker(m_access_lock);
    write16_field_locked(bus, device, function, field, value);
}

void HostController::write32_field(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field, u32 value)
{
    SpinlockLocker locker(m_access_lock);
    write32_field_locked(bus, device, function, field, value);
}

u8 HostController::read8_field(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field)
{
    SpinlockLocker locker(m_access_lock);
    return read8_field_locked(bus, device, function, field);
}

u16 HostController::read16_field(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field)
{
    SpinlockLocker locker(m_access_lock);
    return read16_field_locked(bus, device, function, field);
}

u32 HostController::read32_field(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field)
{
    SpinlockLocker locker(m_access_lock);
    return read32_field_locked(bus, device, function, field);
}

UNMAP_AFTER_INIT Optional<u8> HostController::get_capabilities_pointer_for_function(BusNumber bus, DeviceNumber device, FunctionNumber function)
{
    if (read16_field(bus, device, function, PCI::RegisterOffset::STATUS) & (1 << 4)) {
        return read8_field(bus, device, function, PCI::RegisterOffset::CAPABILITIES_POINTER);
    }
    return {};
}

UNMAP_AFTER_INIT Vector<Capability> HostController::get_capabilities_for_function(BusNumber bus, DeviceNumber device, FunctionNumber function)
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

void HostController::write8_field(BusNumber bus, DeviceNumber device, FunctionNumber function, PCI::RegisterOffset field, u8 value)
{
    write8_field(bus, device, function, to_underlying(field), value);
}

void HostController::write16_field(BusNumber bus, DeviceNumber device, FunctionNumber function, RegisterOffset field, u16 value)
{
    write16_field(bus, device, function, to_underlying(field), value);
}

void HostController::write32_field(BusNumber bus, DeviceNumber device, FunctionNumber function, RegisterOffset field, u32 value)
{
    write32_field(bus, device, function, to_underlying(field), value);
}

u8 HostController::read8_field(BusNumber bus, DeviceNumber device, FunctionNumber function, PCI::RegisterOffset field)
{
    return read8_field(bus, device, function, to_underlying(field));
}
u16 HostController::read16_field(BusNumber bus, DeviceNumber device, FunctionNumber function, PCI::RegisterOffset field)
{
    return read16_field(bus, device, function, to_underlying(field));
}

UNMAP_AFTER_INIT void HostController::enumerate_functions(Function<void(EnumerableDeviceIdentifier const&)> const& callback, Function<void(EnumerableDeviceIdentifier const&)>& post_bridge_callback, BusNumber bus, DeviceNumber device, FunctionNumber function, bool recursive_search_into_bridges)
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
    auto device_ientifier = EnumerableDeviceIdentifier { address, id, revision_id, class_code, subclass_code, prog_if, subsystem_id, subsystem_vendor_id, interrupt_line, interrupt_pin, capabilities };
    callback(device_ientifier);

    if (pci_class == (to_underlying(PCI::ClassID::Bridge) << 8 | to_underlying(PCI::Bridge::SubclassID::PCI_TO_PCI))
        && recursive_search_into_bridges
        && (!m_enumerated_buses.get(read8_field(bus, device, function, PCI::RegisterOffset::SECONDARY_BUS)))) {
        u8 secondary_bus = read8_field(bus, device, function, PCI::RegisterOffset::SECONDARY_BUS);
        dbgln_if(PCI_DEBUG, "PCI: Found secondary bus: {}", secondary_bus);
        VERIFY(secondary_bus != bus);
        m_enumerated_buses.set(secondary_bus, true);
        enumerate_bus(callback, post_bridge_callback, secondary_bus, recursive_search_into_bridges);

        if (post_bridge_callback)
            post_bridge_callback(device_ientifier);
    }
}

UNMAP_AFTER_INIT void HostController::enumerate_device(Function<void(EnumerableDeviceIdentifier const&)> const& callback, Function<void(EnumerableDeviceIdentifier const&)>& post_bridge_callback, BusNumber bus, DeviceNumber device, bool recursive_search_into_bridges)
{
    dbgln_if(PCI_DEBUG, "PCI: Enumerating device in bus={}, device={}", bus, device);
    if (read16_field(bus, device, 0, PCI::RegisterOffset::VENDOR_ID) == PCI::none_value)
        return;
    enumerate_functions(callback, post_bridge_callback, bus, device, 0, recursive_search_into_bridges);
    if (!(read8_field(bus, device, 0, PCI::RegisterOffset::HEADER_TYPE) & 0x80))
        return;
    for (u8 function = 1; function < 8; ++function) {
        if (read16_field(bus, device, function, PCI::RegisterOffset::VENDOR_ID) != PCI::none_value)
            enumerate_functions(callback, post_bridge_callback, bus, device, function, recursive_search_into_bridges);
    }
}

UNMAP_AFTER_INIT void HostController::enumerate_bus(Function<void(EnumerableDeviceIdentifier const&)> const& callback, Function<void(EnumerableDeviceIdentifier const&)>& post_bridge_callback, BusNumber bus, bool recursive_search_into_bridges)
{
    dbgln_if(PCI_DEBUG, "PCI: Enumerating bus {}", bus);
    for (u8 device = 0; device < 32; ++device)
        enumerate_device(callback, post_bridge_callback, bus, device, recursive_search_into_bridges);
}

UNMAP_AFTER_INIT void HostController::enumerate_attached_devices(Function<void(EnumerableDeviceIdentifier const&)> callback, Function<void(EnumerableDeviceIdentifier const&)> post_bridge_callback)
{
    VERIFY(Access::the().access_lock().is_locked());
    VERIFY(Access::the().scan_lock().is_locked());
    m_enumerated_buses.fill(false);
    // First scan bus 0. Find any device on that bus, and if it's a PCI-to-PCI
    // bridge, recursively scan it too.
    m_enumerated_buses.set(m_domain.start_bus(), true);
    enumerate_bus(callback, post_bridge_callback, m_domain.start_bus(), true);

    // Handle Multiple PCI host bridges on bus 0, device 0, functions 1-7 (function 0
    // is the main host bridge).
    // If we happen to miss some PCI buses because they are not reachable through
    // recursive PCI-to-PCI bridges starting from bus 0, we might find them here.
    if ((read8_field(0, 0, 0, PCI::RegisterOffset::HEADER_TYPE) & 0x80) != 0) {
        for (int bus_as_function_number = 1; bus_as_function_number < 8; ++bus_as_function_number) {
            if (read16_field(0, 0, bus_as_function_number, PCI::RegisterOffset::VENDOR_ID) == PCI::none_value)
                continue;
            if (read8_field(0, 0, bus_as_function_number, PCI::RegisterOffset::CLASS) != to_underlying(PCI::ClassID::Bridge))
                continue;
            if (Checked<u8>::addition_would_overflow(m_domain.start_bus(), bus_as_function_number))
                break;
            if (m_enumerated_buses.get(m_domain.start_bus() + bus_as_function_number))
                continue;
            enumerate_bus(callback, post_bridge_callback, m_domain.start_bus() + bus_as_function_number, false);
            m_enumerated_buses.set(m_domain.start_bus() + bus_as_function_number, true);
        }
    }
}

void HostController::configure_attached_devices(PCIConfiguration& config)
{
    // First, Assign PCI-to-PCI bridge bus numbering
    u8 bus_id = 0;
    enumerate_attached_devices([this, &bus_id](EnumerableDeviceIdentifier const& device_identifier) {
        // called for each PCI device encountered (including bridges)
        if (read8_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), PCI::RegisterOffset::CLASS) != PCI::ClassID::Bridge)
            return;
        if (read8_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), PCI::RegisterOffset::SUBCLASS) != PCI::Bridge::SubclassID::PCI_TO_PCI)
            return;
        write8_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), PCI::RegisterOffset::SECONDARY_BUS, ++bus_id);
        write8_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), PCI::RegisterOffset::SUBORDINATE_BUS, 0xFF); },
        [this, &bus_id](EnumerableDeviceIdentifier const& device_identifier) {
            // called after a bridge was recursively enumerated
            write8_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), PCI::RegisterOffset::SUBORDINATE_BUS, bus_id);
        });

    // Second, Assign BAR addresses & Interrupt numbers
    // TODO: We currently naively assign addresses bump-allocator style - Switch to a proper allocator if this is not good enough
    enumerate_attached_devices([this, &config](EnumerableDeviceIdentifier const& device_identifier) {
        // device-generic handling
        auto header_type = read8_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), PCI::RegisterOffset::HEADER_TYPE);
        auto const max_bar = (header_type == 0) ? RegisterOffset::BAR5 : RegisterOffset::BAR1;
        for (u32 bar_offset = to_underlying(RegisterOffset::BAR0); bar_offset <= to_underlying(max_bar); bar_offset += 4) {
            auto bar_value = read32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), bar_offset);
            auto bar_space_type = get_BAR_space_type(bar_value);
            auto bar_prefetchable = (bar_value >> 3) & 1;
            if (bar_space_type != BARSpaceType::Memory32BitSpace && bar_space_type != BARSpaceType::Memory64BitSpace)
                continue; // We only support memory-mapped BAR configuration at the moment
            if (bar_space_type == BARSpaceType::Memory32BitSpace) {
                write32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), bar_offset, 0xFFFFFFFF);
                auto bar_size = read32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), bar_offset);
                write32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), bar_offset, bar_value);
                bar_size &= bar_address_mask;
                bar_size = (~bar_size) + 1;
                if (bar_size == 0)
                    continue;
                auto mmio_32bit_address = align_up_to(config.mmio_32bit_base, bar_size);
                if (mmio_32bit_address + bar_size <= config.mmio_32bit_end) {
                    write32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), bar_offset, mmio_32bit_address);
                    config.mmio_32bit_base = mmio_32bit_address + bar_size;
                    continue;
                }
                auto mmio_64bit_address = align_up_to(config.mmio_64bit_base, bar_size);
                if (bar_prefetchable && mmio_64bit_address + bar_size <= config.mmio_64bit_end && mmio_64bit_address + bar_size <= NumericLimits<u32>::max()) {
                    write32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), bar_offset, mmio_64bit_address);
                    config.mmio_64bit_base = mmio_64bit_address + bar_size;
                    continue;
                }
                dmesgln("PCI: Ran out of 32-bit MMIO address space");
                VERIFY_NOT_REACHED();
            }
            // 64-bit space
            auto next_bar_value = read32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), bar_offset + 4);
            write32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), bar_offset, 0xFFFFFFFF);
            write32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), bar_offset + 4, 0xFFFFFFFF);
            u64 bar_size = read32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), bar_offset);
            bar_size |= (u64)read32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), bar_offset + 4) << 32;
            write32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), bar_offset, bar_value);
            write32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), bar_offset + 4, next_bar_value);
            bar_size &= bar_address_mask;
            bar_size = (~bar_size) + 1;
            if (bar_size == 0) {
                bar_offset += 4;
                continue;
            }
            auto mmio_64bit_address = align_up_to(config.mmio_64bit_base, bar_size);
            if (bar_prefetchable && mmio_64bit_address + bar_size <= config.mmio_64bit_end) {
                write32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), bar_offset, mmio_64bit_address & 0xFFFFFFFF);
                write32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), bar_offset + 4, mmio_64bit_address >> 32);
                config.mmio_64bit_base = mmio_64bit_address + bar_size;
                bar_offset += 4;
                continue;
            }
            auto mmio_32bit_address = align_up_to(config.mmio_32bit_base, bar_size);
            if (mmio_32bit_address + bar_size <= config.mmio_32bit_end) {
                write32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), bar_offset, mmio_32bit_address & 0xFFFFFFFF);
                write32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), bar_offset + 4, mmio_32bit_address >> 32);
                config.mmio_32bit_base = mmio_32bit_address + bar_size;
                bar_offset += 4;
                continue;
            }
            dmesgln("PCI: Ran out of 64-bit MMIO address space");
            VERIFY_NOT_REACHED();
        }
        // enable memory space
        auto command_value = read16_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), PCI::RegisterOffset::COMMAND);
        command_value |= 1 << 1; // memory space enable
        write16_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), PCI::RegisterOffset::COMMAND, command_value);
        // assign interrupt number
        auto interrupt_pin = read8_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), PCI::RegisterOffset::INTERRUPT_PIN);
        auto masked_identifier = PCIInterruptSpecifier{
            .interrupt_pin = interrupt_pin,
            .function = device_identifier.address().function(),
            .device = device_identifier.address().device(),
            .bus = device_identifier.address().bus()
        };
        masked_identifier &= config.interrupt_mask;
        auto interrupt_number = config.masked_interrupt_mapping.get(masked_identifier);
        if (interrupt_number.has_value())
            write8_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), PCI::RegisterOffset::INTERRUPT_LINE, interrupt_number.value());

        if (read8_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), PCI::RegisterOffset::CLASS) != PCI::ClassID::Bridge)
            return;
        if (read8_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), PCI::RegisterOffset::SUBCLASS) != PCI::Bridge::SubclassID::PCI_TO_PCI)
            return;
        // bridge-specific handling
        config.mmio_32bit_base = align_up_to(config.mmio_32bit_base, MiB);
        config.mmio_64bit_base = align_up_to(config.mmio_64bit_base, MiB);
        write32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), PCI::RegisterOffset::MEMORY_BASE, config.mmio_32bit_base >> 16);
        write32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), PCI::RegisterOffset::PREFETCHABLE_MEMORY_BASE, config.mmio_64bit_base >> 16);
        write32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), PCI::RegisterOffset::PREFETCHABLE_MEMORY_BASE_UPPER_32_BITS, config.mmio_64bit_base >> 32); },
        [this, &config](EnumerableDeviceIdentifier const& device_identifier) {
            // called after a bridge was recursively enumerated
            config.mmio_32bit_base = align_up_to(config.mmio_32bit_base, MiB);
            config.mmio_64bit_base = align_up_to(config.mmio_64bit_base, MiB);
            write32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), PCI::RegisterOffset::MEMORY_LIMIT, config.mmio_32bit_base >> 16);
            write32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), PCI::RegisterOffset::PREFETCHABLE_MEMORY_LIMIT, config.mmio_64bit_base >> 16);
            write32_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), PCI::RegisterOffset::PREFETCHABLE_MEMORY_LIMIT_UPPER_32_BITS, config.mmio_64bit_base >> 32);
            // enable bridging
            auto command_value = read16_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), PCI::RegisterOffset::COMMAND);
            command_value |= 1 << 2; // enable forwarding of requests by the bridge
            write16_field(device_identifier.address().bus(), device_identifier.address().device(), device_identifier.address().function(), PCI::RegisterOffset::COMMAND, command_value);
        });
}

}
