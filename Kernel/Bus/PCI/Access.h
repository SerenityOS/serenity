/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/Vector.h>
#include <Kernel/Bus/PCI/Controller/HostController.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/FileSystem/SysFS.h>
#include <Kernel/Locking/Spinlock.h>

namespace Kernel::PCI {

class HostBridge;
class Access {
public:
    static bool initialize_for_multiple_pci_domains(PhysicalAddress mcfg_table);
    static bool initialize_for_one_pci_domain();

    void fast_enumerate(Function<void(DeviceIdentifier const&)>&) const;
    void rescan_hardware();

    static Access& the();
    static bool is_initialized();

    void write8_field(Address address, u32 field, u8 value);
    void write16_field(Address address, u32 field, u16 value);
    void write32_field(Address address, u32 field, u32 value);
    u8 read8_field(Address address, u32 field);
    u16 read16_field(Address address, u32 field);
    u32 read32_field(Address address, u32 field);
    DeviceIdentifier get_device_identifier(Address address) const;

    Spinlock const& scan_lock() const { return m_scan_lock; }
    RecursiveSpinlock const& access_lock() const { return m_access_lock; }

    void add_host_controller_and_enumerate_attached_devices(NonnullOwnPtr<HostController>, Function<void(DeviceIdentifier const&)> callback);

private:
    u8 read8_field(Address address, RegisterOffset field);
    u16 read16_field(Address address, RegisterOffset field);

    void add_host_controller(NonnullOwnPtr<HostController>);
    bool find_and_register_pci_host_bridges_from_acpi_mcfg_table(PhysicalAddress mcfg);
    Access();

    Vector<Capability> get_capabilities(Address);
    Optional<u8> get_capabilities_pointer(Address address);

    mutable RecursiveSpinlock m_access_lock;
    mutable Spinlock m_scan_lock;

    HashMap<u32, NonnullOwnPtr<HostController>> m_host_controllers;
    Vector<DeviceIdentifier> m_device_identifiers;
};
}
