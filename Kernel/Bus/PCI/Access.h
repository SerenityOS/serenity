/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Try.h>
#include <AK/Vector.h>
#include <Kernel/Bus/PCI/Controller/HostController.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Bus/PCI/Initializer.h>
#include <Kernel/Locking/Spinlock.h>

namespace Kernel::PCI {

class Access {
public:
    static bool initialize_for_multiple_pci_domains(PhysicalAddress mcfg_table);

#if ARCH(X86_64)
    static bool initialize_for_one_pci_domain();
#endif

    ErrorOr<void> fast_enumerate(Function<void(DeviceIdentifier const&)>&) const;
    void configure_pci_space(PCIConfiguration&);
    void rescan_hardware();

    static Access& the();
    static bool is_initialized();
    static bool is_disabled();
    static bool is_hardware_disabled();

    void write8_field(DeviceIdentifier const&, u32 field, u8 value);
    void write16_field(DeviceIdentifier const&, u32 field, u16 value);
    void write32_field(DeviceIdentifier const&, u32 field, u32 value);
    u8 read8_field(DeviceIdentifier const&, u32 field);
    u16 read16_field(DeviceIdentifier const&, u32 field);
    u32 read32_field(DeviceIdentifier const&, u32 field);

    // FIXME: Remove this once we can use PCI::Capability with inline buffer
    // so we don't need this method
    DeviceIdentifier const& get_device_identifier(Address address) const;

    Spinlock<LockRank::None> const& scan_lock() const { return m_scan_lock; }
    RecursiveSpinlock<LockRank::None> const& access_lock() const { return m_access_lock; }

    ErrorOr<void> add_host_controller_and_scan_for_devices(NonnullOwnPtr<HostController>);

private:
    friend void PCI::initialize();

    u8 read8_field(DeviceIdentifier const&, RegisterOffset field);
    u16 read16_field(DeviceIdentifier const&, RegisterOffset field);

    void add_host_controller(NonnullOwnPtr<HostController>);
    bool find_and_register_pci_host_bridges_from_acpi_mcfg_table(PhysicalAddress mcfg);
    Access();

    mutable RecursiveSpinlock<LockRank::None> m_access_lock {};
    mutable Spinlock<LockRank::None> m_scan_lock {};

    HashMap<u32, NonnullOwnPtr<PCI::HostController>> m_host_controllers;
    Vector<NonnullRefPtr<DeviceIdentifier>> m_device_identifiers;
};
}
