/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <AK/Error.h>
#include <AK/HashTable.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Controller/HostBridge.h>
#include <Kernel/Bus/PCI/Controller/MemoryBackedHostBridge.h>
#include <Kernel/Debug.h>
#include <Kernel/Firmware/ACPI/Definitions.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>

namespace Kernel::PCI {

#define PCI_MMIO_CONFIG_SPACE_SIZE 4096

static Access* s_access;

Access& Access::the()
{
    if (s_access == nullptr) {
        VERIFY_NOT_REACHED(); // We failed to initialize the PCI subsystem, so stop here!
    }
    return *s_access;
}

bool Access::is_initialized()
{
    return (s_access != nullptr);
}

UNMAP_AFTER_INIT bool Access::find_and_register_pci_host_bridges_from_acpi_mcfg_table(PhysicalAddress mcfg_table)
{
    u32 length = 0;
    u8 revision = 0;
    {
        auto mapped_mcfg_table_or_error = Memory::map_typed<ACPI::Structures::SDTHeader>(mcfg_table);
        if (mapped_mcfg_table_or_error.is_error()) {
            dbgln("Failed to map MCFG table");
            return false;
        }
        auto mapped_mcfg_table = mapped_mcfg_table_or_error.release_value();
        length = mapped_mcfg_table->length;
        revision = mapped_mcfg_table->revision;
    }

    if (length == sizeof(ACPI::Structures::SDTHeader))
        return false;

    dbgln("PCI: MCFG, length: {}, revision: {}", length, revision);

    if (Checked<size_t>::addition_would_overflow(length, PAGE_SIZE)) {
        dbgln("Overflow when adding extra page to allocation of length {}", length);
        return false;
    }
    length += PAGE_SIZE;
    auto region_size_or_error = Memory::page_round_up(length);
    if (region_size_or_error.is_error()) {
        dbgln("Failed to round up length of {} to pages", length);
        return false;
    }
    auto mcfg_region_or_error = MM.allocate_kernel_region(mcfg_table.page_base(), region_size_or_error.value(), "PCI Parsing MCFG", Memory::Region::Access::ReadWrite);
    if (mcfg_region_or_error.is_error())
        return false;
    auto& mcfg = *(ACPI::Structures::MCFG*)mcfg_region_or_error.value()->vaddr().offset(mcfg_table.offset_in_page()).as_ptr();
    dbgln_if(PCI_DEBUG, "PCI: Checking MCFG @ {}, {}", VirtualAddress(&mcfg), mcfg_table);
    for (u32 index = 0; index < ((mcfg.header.length - sizeof(ACPI::Structures::MCFG)) / sizeof(ACPI::Structures::PCI_MMIO_Descriptor)); index++) {
        u8 start_bus = mcfg.descriptors[index].start_pci_bus;
        u8 end_bus = mcfg.descriptors[index].end_pci_bus;
        u64 start_addr = mcfg.descriptors[index].base_addr;

        Domain pci_domain { index, start_bus, end_bus };
        dmesgln("PCI: New PCI domain @ {}, PCI buses ({}-{})", PhysicalAddress { start_addr }, start_bus, end_bus);
        auto host_bridge = MemoryBackedHostBridge::must_create(pci_domain, PhysicalAddress { start_addr });
        add_host_controller(move(host_bridge));
    }

    return true;
}

UNMAP_AFTER_INIT bool Access::initialize_for_multiple_pci_domains(PhysicalAddress mcfg_table)
{
    VERIFY(!Access::is_initialized());
    auto* access = new Access();
    if (!access->find_and_register_pci_host_bridges_from_acpi_mcfg_table(mcfg_table))
        return false;
    access->rescan_hardware();
    dbgln_if(PCI_DEBUG, "PCI: access for multiple PCI domain initialised.");
    return true;
}

UNMAP_AFTER_INIT bool Access::initialize_for_one_pci_domain()
{
    VERIFY(!Access::is_initialized());
    auto* access = new Access();
    auto host_bridge = HostBridge::must_create_with_io_access();
    access->add_host_controller(move(host_bridge));
    access->rescan_hardware();
    dbgln_if(PCI_DEBUG, "PCI: access for one PCI domain initialised.");
    return true;
}

void Access::add_host_controller_and_enumerate_attached_devices(NonnullOwnPtr<HostController> controller, Function<void(DeviceIdentifier const&)> callback)
{
    SpinlockLocker locker(m_access_lock);
    SpinlockLocker scan_locker(m_scan_lock);
    auto domain_number = controller->domain_number();

    VERIFY(!m_host_controllers.contains(domain_number));
    // Note: We need to register the new controller as soon as possible, and
    // definitely before enumerating devices behing that.
    m_host_controllers.set(domain_number, move(controller));
    m_host_controllers.get(domain_number).value()->enumerate_attached_devices([&](DeviceIdentifier const& device_identifier) -> void {
        m_device_identifiers.append(device_identifier);
        callback(device_identifier);
    });
}

UNMAP_AFTER_INIT void Access::add_host_controller(NonnullOwnPtr<HostController> controller)
{
    auto domain_number = controller->domain_number();
    m_host_controllers.set(domain_number, move(controller));
}

UNMAP_AFTER_INIT Access::Access()
{
    s_access = this;
}

UNMAP_AFTER_INIT void Access::rescan_hardware()
{
    SpinlockLocker locker(m_access_lock);
    SpinlockLocker scan_locker(m_scan_lock);
    VERIFY(m_device_identifiers.is_empty());
    for (auto it = m_host_controllers.begin(); it != m_host_controllers.end(); ++it) {
        (*it).value->enumerate_attached_devices([this](DeviceIdentifier device_identifier) -> void {
            m_device_identifiers.append(device_identifier);
        });
    }
}

void Access::fast_enumerate(Function<void(DeviceIdentifier const&)>& callback) const
{
    SpinlockLocker locker(m_access_lock);
    VERIFY(!m_device_identifiers.is_empty());
    for (auto const& device_identifier : m_device_identifiers) {
        callback(device_identifier);
    }
}

DeviceIdentifier Access::get_device_identifier(Address address) const
{
    for (auto device_identifier : m_device_identifiers) {
        if (device_identifier.address().domain() == address.domain()
            && device_identifier.address().bus() == address.bus()
            && device_identifier.address().device() == address.device()
            && device_identifier.address().function() == address.function()) {
            return device_identifier;
        }
    }
    VERIFY_NOT_REACHED();
}

void Access::write8_field(Address address, u32 field, u8 value)
{
    SpinlockLocker locker(m_access_lock);
    VERIFY(m_host_controllers.contains(address.domain()));
    auto& controller = *m_host_controllers.get(address.domain()).value();
    controller.write8_field(address.bus(), address.device(), address.function(), field, value);
}
void Access::write16_field(Address address, u32 field, u16 value)
{
    SpinlockLocker locker(m_access_lock);
    VERIFY(m_host_controllers.contains(address.domain()));
    auto& controller = *m_host_controllers.get(address.domain()).value();
    controller.write16_field(address.bus(), address.device(), address.function(), field, value);
}
void Access::write32_field(Address address, u32 field, u32 value)
{
    SpinlockLocker locker(m_access_lock);
    VERIFY(m_host_controllers.contains(address.domain()));
    auto& controller = *m_host_controllers.get(address.domain()).value();
    controller.write32_field(address.bus(), address.device(), address.function(), field, value);
}

u8 Access::read8_field(Address address, RegisterOffset field)
{
    return read8_field(address, to_underlying(field));
}
u16 Access::read16_field(Address address, RegisterOffset field)
{
    return read16_field(address, to_underlying(field));
}

u8 Access::read8_field(Address address, u32 field)
{
    SpinlockLocker locker(m_access_lock);
    VERIFY(m_host_controllers.contains(address.domain()));
    auto& controller = *m_host_controllers.get(address.domain()).value();
    return controller.read8_field(address.bus(), address.device(), address.function(), field);
}
u16 Access::read16_field(Address address, u32 field)
{
    SpinlockLocker locker(m_access_lock);
    VERIFY(m_host_controllers.contains(address.domain()));
    auto& controller = *m_host_controllers.get(address.domain()).value();
    return controller.read16_field(address.bus(), address.device(), address.function(), field);
}
u32 Access::read32_field(Address address, u32 field)
{
    SpinlockLocker locker(m_access_lock);
    VERIFY(m_host_controllers.contains(address.domain()));
    auto& controller = *m_host_controllers.get(address.domain()).value();
    return controller.read32_field(address.bus(), address.device(), address.function(), field);
}

}
