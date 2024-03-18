/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Bitmap.h>
#include <AK/Vector.h>
#include <Kernel/Bus/PCI/Bus.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Locking/Spinlock.h>

namespace Kernel::PCI {

class HostController
    : public AtomicRefCounted<HostController> {
    friend class Access;

public:
    virtual ~HostController() = default;

    ErrorOr<void> enumerate_all_devices(Badge<Access>);

    u32 domain_number() const { return m_domain.domain_number(); }

    void write8_field(BusNumber, DeviceNumber, FunctionNumber, u32 field, u8 value);
    void write16_field(BusNumber, DeviceNumber, FunctionNumber, u32 field, u16 value);
    void write32_field(BusNumber, DeviceNumber, FunctionNumber, u32 field, u32 value);

    u8 read8_field(BusNumber, DeviceNumber, FunctionNumber, u32 field);
    u16 read16_field(BusNumber, DeviceNumber, FunctionNumber, u32 field);
    u32 read32_field(BusNumber, DeviceNumber, FunctionNumber, u32 field);

private:
    using DevicesList = SpinlockProtected<IntrusiveList<&Device::m_host_controller_list_node>, LockRank::None>;

    ErrorOr<void> enumerate_bus(Bus&, bool recursive);
    ErrorOr<void> enumerate_function(Bus&, DeviceNumber, FunctionNumber, bool recursive);
    ErrorOr<void> enumerate_device(Bus&, DeviceNumber, bool recursive);

    void fill_device_resources(PCI::Device& device);
    PCI::Resource determine_device_resource_address_and_length(PCI::Device& device, u8 field);

    u8 read8_field_locked(BusNumber, DeviceNumber, FunctionNumber, RegisterOffset field);
    u16 read16_field_locked(BusNumber, DeviceNumber, FunctionNumber, RegisterOffset field);

    Optional<u8> get_capabilities_pointer_for_function(Bus&, DeviceNumber, FunctionNumber);

    void enumerate_msi_capabilities_for_function(Bus&, PCI::Device&, Vector<PCI::Capability> const&);

    size_t calculate_bar_resource_space_size(Bus& bus, DeviceNumber device, FunctionNumber function, size_t resource_index);

protected:
    virtual void write8_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field, u8 value) = 0;
    virtual void write16_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field, u16 value) = 0;
    virtual void write32_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field, u32 value) = 0;

    virtual u8 read8_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field) = 0;
    virtual u16 read16_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field) = 0;
    virtual u32 read32_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field) = 0;

    explicit HostController(PCI::Domain const& domain, NonnullRefPtr<Bus> root_bus);

    ErrorOr<void> enumerate_capabilities_for_function(Bus&, PCI::Device&);

    PCI::Domain const m_domain;
    NonnullRefPtr<Bus> const m_root_bus;

    Spinlock<LockRank::None> m_access_lock;

private:
    DevicesList m_attached_devices;
    Bitmap m_enumerated_buses;
};
}
