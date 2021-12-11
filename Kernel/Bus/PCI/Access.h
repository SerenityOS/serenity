/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/Vector.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/FileSystem/SysFS.h>
#include <Kernel/Locking/Spinlock.h>

namespace Kernel::PCI {

class Access {
public:
    enum class AccessType {
        IO,
        Memory,
    };

    static bool initialize_for_memory_access(PhysicalAddress mcfg_table);
    static bool initialize_for_io_access();

    void fast_enumerate(Function<void(DeviceIdentifier const&)>&) const;
    void rescan_hardware();
    void rescan_hardware_with_memory_addressing();
    void rescan_hardware_with_io_addressing();

    static Access& the();
    static bool is_initialized();

    void write8_field(Address address, u32 field, u8 value);
    void write16_field(Address address, u32 field, u16 value);
    void write32_field(Address address, u32 field, u32 value);
    u8 read8_field(Address address, u32 field);
    u16 read16_field(Address address, u32 field);
    u32 read32_field(Address address, u32 field);
    DeviceIdentifier get_device_identifier(Address address) const;

private:
    u8 read8_field(Address address, RegisterOffset field);
    u16 read16_field(Address address, RegisterOffset field);

    void enumerate_bus(int type, u8 bus, bool recursive);
    void enumerate_functions(int type, u8 bus, u8 device, u8 function, bool recursive);
    void enumerate_device(int type, u8 bus, u8 device, bool recursive);

    explicit Access(AccessType);
    bool search_pci_domains_from_acpi_mcfg_table(PhysicalAddress mcfg);
    Vector<Capability> get_capabilities(Address);
    Optional<u8> get_capabilities_pointer(Address address);

    // IO access (legacy) operations
    u8 io_read8_field(Address address, u32 field);
    u16 io_read16_field(Address address, u32 field);
    u32 io_read32_field(Address address, u32 field);
    void io_write8_field(Address address, u32, u8);
    void io_write16_field(Address address, u32, u16);
    void io_write32_field(Address address, u32, u32);
    u16 io_read_type(Address address);

    // Memory-mapped access operations
    void map_bus_region(u32 domain, u8 bus);
    u8 memory_read8_field(Address address, u32 field);
    u16 memory_read16_field(Address address, u32 field);
    u32 memory_read32_field(Address address, u32 field);
    void memory_write8_field(Address address, u32, u8);
    void memory_write16_field(Address address, u32, u16);
    void memory_write32_field(Address address, u32, u32);
    u16 memory_read_type(Address address);
    VirtualAddress get_device_configuration_memory_mapped_space(Address address);
    Optional<PhysicalAddress> determine_memory_mapped_bus_base_address(u32 domain, u8 bus) const;

    // Data-members for accessing Memory mapped PCI devices' configuration spaces
    u8 m_mapped_bus { 0 };
    OwnPtr<Memory::Region> m_mapped_bus_region;
    HashMap<u32, PCI::Domain> m_domains;

    // General Data-members
    mutable Mutex m_access_lock;
    mutable Spinlock m_scan_lock;
    Bitmap m_enumerated_buses;
    AccessType m_access_type;
    Vector<DeviceIdentifier> m_device_identifiers;
};
}
