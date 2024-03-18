/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Controller/HostController.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Bus/PCI/Resource.h>
#include <Kernel/Sections.h>

namespace Kernel::PCI {

HostController::HostController(PCI::Domain const& domain, NonnullRefPtr<Bus> root_bus)
    : m_domain(domain)
    , m_root_bus(root_bus)
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

void HostController::fill_device_resources(PCI::Device& device)
{
    VERIFY(m_access_lock.is_locked());
    auto determine_device_resource_characteristics = [this](PCI::Device& device, PCI::Resource& resource, u8 field) -> void {
        auto device_address = device.device_id().address();
        auto bus_number = device_address.bus();
        auto device_number = device_address.device();
        auto function_number = device_address.function();
        u32 bar_reserved = read32_field_locked(bus_number, device_number, function_number, field);
        // NOTE: For IO space, bit 0 is set to 1.
        if (bar_reserved & (1 << 0)) {
            resource.type = PCI::Resource::SpaceType::IOSpace;
        } else {
            auto memory_space_type = (bar_reserved >> 1) & 0b11;
            switch (memory_space_type) {
            case 0:
                resource.type = PCI::Resource::SpaceType::Memory32BitSpace;
                break;
            case 1:
                resource.type = PCI::Resource::SpaceType::Memory16BitSpace;
                break;
            case 2:
                resource.type = PCI::Resource::SpaceType::Memory64BitSpace;
                break;
            default:
                VERIFY_NOT_REACHED();
            }
        }

        write32_field_locked(bus_number, device_number, function_number, field, 0xFFFFFFFF);
        u32 space_size = read32_field_locked(bus_number, device_number, function_number, field);
        write32_field_locked(bus_number, device_number, function_number, field, bar_reserved);
        space_size &= bar_address_mask;
        resource.length = (~space_size) + 1;
        resource.address = bar_reserved;
    };

    SpinlockLocker locker(device.m_operation_lock);
    for (size_t index = 0; index < device.m_resources.size(); index++) {
        PCI::Resource resource {};
        determine_device_resource_characteristics(
            device,
            resource,
            to_underlying(PCI::RegisterOffset::BAR0) + (index << 2));
        device.m_resources[index] = resource;
        if (resource.length != 0 && resource.address != 0) {
            dbgln("{}: {} BAR {}, @ {:x} (length {})",
                device.device_id().address(),
                resource.type == Resource::SpaceType::IOSpace ? "IO"sv : "Memory"sv,
                index,
                resource.address,
                resource.length);
        }
    }

    PCI::Resource expansion_rom_resource {};
    determine_device_resource_characteristics(device, expansion_rom_resource, to_underlying(PCI::RegisterOffset::EXPANSION_ROM_POINTER));
    // FIXME: Is it OK to assume that the expansion ROM will always appear in the 32 bit space?
    expansion_rom_resource.type = PCI::Resource::SpaceType::Memory32BitSpace;
    device.m_expansion_rom_resource = expansion_rom_resource;
}

ErrorOr<void> HostController::enumerate_function(Bus& bus, DeviceNumber device, FunctionNumber function, bool recursive_search_into_bridges)
{
    VERIFY(m_access_lock.is_locked());
    dbgln_if(PCI_DEBUG, "PCI: Enumerating function, bus={}, device={}, function={}", bus, device, function);
    auto pci_class = (read8_field_locked(bus.number(), device, function, PCI::RegisterOffset::CLASS) << 8u) | read8_field_locked(bus.number(), device, function, PCI::RegisterOffset::SUBCLASS);

    Address address(domain_number(), bus.number().value(), device.value(), function.value());
    HardwareID id = { read16_field_locked(bus.number(), device, function, PCI::RegisterOffset::VENDOR_ID), read16_field_locked(bus.number(), device, function, PCI::RegisterOffset::DEVICE_ID) };
    ClassCode class_code = read8_field_locked(bus.number(), device, function, PCI::RegisterOffset::CLASS);
    SubclassCode subclass_code = read8_field_locked(bus.number(), device, function, PCI::RegisterOffset::SUBCLASS);
    ProgrammingInterface prog_if = read8_field_locked(bus.number(), device, function, PCI::RegisterOffset::PROG_IF);
    RevisionID revision_id = read8_field_locked(bus.number(), device, function, PCI::RegisterOffset::REVISION_ID);
    SubsystemID subsystem_id = read16_field_locked(bus.number(), device, function, PCI::RegisterOffset::SUBSYSTEM_ID);
    SubsystemVendorID subsystem_vendor_id = read16_field_locked(bus.number(), device, function, PCI::RegisterOffset::SUBSYSTEM_VENDOR_ID);
    InterruptLine interrupt_line = read8_field_locked(bus.number(), device, function, PCI::RegisterOffset::INTERRUPT_LINE);
    InterruptPin interrupt_pin = read8_field_locked(bus.number(), device, function, PCI::RegisterOffset::INTERRUPT_PIN);
    auto identifier = EnumerableDeviceIdentifier { address, id, revision_id, class_code, subclass_code, prog_if, subsystem_id, subsystem_vendor_id, interrupt_line, interrupt_pin };

    auto enumerable_device = TRY(Device::from_enumerable_identifier(identifier));
    enumerable_device->set_parent_bus(bus);
    enumerable_device->set_host_controller(*this);
    if (pci_class != (to_underlying(PCI::ClassID::Bridge) << 8 | to_underlying(PCI::Bridge::SubclassID::PCI_TO_PCI))) {
        dbgln("PCI [{:04X}:{:02X}:{:02X}:{:02X}]: vendor, dev_id {:04x}:{:04x}",
            (u32)m_domain.domain_number(),
            (u8)bus.number().value(),
            (u8)device.value(),
            (u8)function.value(),
            (u16)id.vendor_id,
            (u16)id.device_id);
        fill_device_resources(enumerable_device);
    }

    TRY(enumerate_capabilities_for_function(bus, enumerable_device));
    bus.attach_child_device(enumerable_device);
    m_attached_devices.with([enumerable_device](auto& list) {
        list.append(*enumerable_device);
    });
    PCI::Access::the().register_device(*enumerable_device);

    if (pci_class == (to_underlying(PCI::ClassID::Bridge) << 8 | to_underlying(PCI::Bridge::SubclassID::PCI_TO_PCI))
        && recursive_search_into_bridges
        && (!m_enumerated_buses.get(read8_field_locked(bus.number(), device, function, PCI::RegisterOffset::SECONDARY_BUS)))) {
        // NOTE: New bus appear under this host bridge, create a new Bus object and scan it!

        u8 secondary_bus_number = read8_field_locked(bus.number(), device, function, PCI::RegisterOffset::SECONDARY_BUS);
        dbgln("PCI [{:04x}:{:02x}:{:02x}:{:02x}]: vendor, dev_id {:04x}:{:04x}, pci-bridge to bus {}",
            (u32)m_domain.domain_number(),
            (u8)bus.number().value(),
            (u8)device.value(),
            (u8)function.value(),
            (u16)id.vendor_id,
            (u16)id.device_id,
            secondary_bus_number);
        VERIFY(secondary_bus_number != bus.number());
        m_enumerated_buses.set(secondary_bus_number, true);

        auto secondary_child_bus = TRY(Bus::create(secondary_bus_number, enumerable_device, bus));
        bus.attach_child_bus(secondary_child_bus);
        TRY(enumerate_bus(secondary_child_bus, recursive_search_into_bridges));
    }
    return {};
}

ErrorOr<void> HostController::enumerate_device(Bus& bus, DeviceNumber device, bool recursive_search_into_bridges)
{
    VERIFY(m_access_lock.is_locked());
    dbgln_if(PCI_DEBUG, "PCI: Enumerating device in bus={}, device={}", bus, device);
    if (read16_field_locked(bus.number(), device, 0, PCI::RegisterOffset::VENDOR_ID) == PCI::none_value)
        return {};
    TRY(enumerate_function(bus, device, 0, recursive_search_into_bridges));
    if (!(read8_field_locked(bus.number(), device, 0, PCI::RegisterOffset::HEADER_TYPE) & 0x80))
        return {};
    for (u8 function = 1; function < 8; ++function) {
        if (read16_field_locked(bus.number(), device, function, PCI::RegisterOffset::VENDOR_ID) != PCI::none_value)
            TRY(enumerate_function(bus, device, function, recursive_search_into_bridges));
    }
    return {};
}

ErrorOr<void> HostController::enumerate_bus(Bus& bus, bool recursive_search_into_bridges)
{
    VERIFY(m_access_lock.is_locked());
    dbgln_if(PCI_DEBUG, "PCI: Enumerating bus {}", bus);
    for (u8 device = 0; device < 32; ++device)
        TRY(enumerate_device(bus, device, recursive_search_into_bridges));
    return {};
}

ErrorOr<void> HostController::enumerate_all_devices(Badge<Access>)
{
    SpinlockLocker locker(m_access_lock);
    // First scan bus 0. Find any device on that bus, and if it's a PCI-to-PCI
    // bridge, recursively scan it too.
    m_enumerated_buses.set(m_domain.start_bus(), true);
    TRY(enumerate_bus(*m_root_bus, true));

    // Handle Multiple PCI host bridges on bus 0, device 0, functions 1-7 (function 0
    // is the main host bridge).
    // If we happen to miss some PCI buses because they are not reachable through
    // recursive PCI-to-PCI bridges starting from bus 0, we might find them here.
    if ((read8_field_locked(m_root_bus->number(), 0, 0, PCI::RegisterOffset::HEADER_TYPE) & 0x80) != 0) {
        for (int bus_as_function_number = 1; bus_as_function_number < 8; ++bus_as_function_number) {
            if (read16_field_locked(m_root_bus->number(), 0, bus_as_function_number, PCI::RegisterOffset::VENDOR_ID) == PCI::none_value)
                continue;
            if (read16_field_locked(m_root_bus->number(), 0, bus_as_function_number, PCI::RegisterOffset::CLASS) != 0x6)
                continue;
            if (Checked<u8>::addition_would_overflow(m_domain.start_bus(), bus_as_function_number))
                break;
            if (m_enumerated_buses.get(m_domain.start_bus() + bus_as_function_number))
                continue;
            auto secondary_child_bus = TRY(Bus::create(m_domain.start_bus() + bus_as_function_number, m_root_bus->self_device(), m_root_bus));
            m_root_bus->attach_child_bus(secondary_child_bus);
            TRY(enumerate_bus(secondary_child_bus, false));
            m_enumerated_buses.set(m_domain.start_bus() + bus_as_function_number, true);
        }
    }
    return {};
}

Optional<u8> HostController::get_capabilities_pointer_for_function(Bus& bus, DeviceNumber device_number, FunctionNumber function_number)
{
    VERIFY(m_access_lock.is_locked());
    if (read16_field_locked(bus.number(), device_number, function_number, PCI::RegisterOffset::STATUS) & (1 << 4)) {
        return read8_field_locked(bus.number(), device_number, function_number, PCI::RegisterOffset::CAPABILITIES_POINTER);
    }
    return {};
}

void HostController::enumerate_msi_capabilities_for_function(Bus& bus, PCI::Device& device, Vector<Capability> const& capabilities)
{
    auto device_number = device.device_id().address().device();
    auto function_number = device.device_id().address().function();
    // NOTE: We can't use the ordinary Capability::read{8,16,32} methods because
    // they will call the HostController methods which will not be operable
    // due to locking.
    // FIXME: Ensure we don't try to address something beyond the 256 byte space
    // on x86 PCI host bridges.
    auto capability_read32 = [this, &bus, device_number, function_number](PCI::Capability& capability, size_t offset) -> u32 {
        return read32_field_locked(bus.number(), device_number, function_number, capability.ptr() + offset);
    };
    auto capability_read16 = [this, &bus, device_number, function_number](PCI::Capability& capability, size_t offset) -> u16 {
        return read16_field_locked(bus.number(), device_number, function_number, capability.ptr() + offset);
    };
    auto capability_read8 = [this, &bus, device_number, function_number](PCI::Capability& capability, size_t offset) -> u8 {
        return read8_field_locked(bus.number(), device_number, function_number, capability.ptr() + offset);
    };

    for (auto cap : capabilities) {
        if (cap.id() == PCI::Capabilities::ID::MSIX) {
            auto msix_bir_bar = (capability_read8(cap, 4) & msix_table_bir_mask);
            auto msix_bir_offset = (capability_read32(cap, 4) & msix_table_offset_mask);
            auto msix_count = (capability_read16(cap, 2) & msix_control_table_mask) + 1;
            device.m_msix_info = MSIxInfo(msix_count, msix_bir_bar, msix_bir_offset);
        }

        if (cap.id() == PCI::Capabilities::ID::MSI) {
            bool message_address_64_bit_format = (capability_read8(cap, msi_control_offset) & msi_address_format_mask);
            u8 count = 1;
            u8 mme_count = (capability_read8(cap, msi_control_offset) & msi_mmc_format_mask) >> 1;
            if (mme_count)
                count = mme_count;
            device.m_msi_info = MSIInfo(message_address_64_bit_format, count);
        }
    }
}

ErrorOr<void> HostController::enumerate_capabilities_for_function(Bus& bus, PCI::Device& device)
{
    VERIFY(m_access_lock.is_locked());
    auto device_number = device.device_id().address().device();
    auto function_number = device.device_id().address().function();
    auto capabilities_pointer = get_capabilities_pointer_for_function(bus, device_number, function_number);
    if (!capabilities_pointer.has_value()) {
        return {};
    }
    Vector<Capability> capabilities;
    auto capability_pointer = capabilities_pointer.value();
    while (capability_pointer != 0) {
        u16 capability_header = read16_field_locked(bus.number(), device_number, function_number, capability_pointer);
        u8 capability_id = capability_header & 0xff;

        auto capability = Capability(device, capability_id, capability_pointer);
        TRY(capabilities.try_append(capability));
        capability_pointer = capability_header >> 8;
    }

    enumerate_msi_capabilities_for_function(bus, device, capabilities);
    device.set_capabilities(move(capabilities));
    return {};
}

u8 HostController::read8_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, PCI::RegisterOffset field)
{
    VERIFY(m_access_lock.is_locked());
    return read8_field_locked(bus, device, function, to_underlying(field));
}
u16 HostController::read16_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, PCI::RegisterOffset field)
{
    VERIFY(m_access_lock.is_locked());
    return read16_field_locked(bus, device, function, to_underlying(field));
}
}
