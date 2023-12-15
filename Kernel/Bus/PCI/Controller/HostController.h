/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/HashMap.h>
#include <AK/Vector.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Locking/Spinlock.h>

namespace Kernel::PCI {

AK_TYPEDEF_DISTINCT_ORDERED_ID(u8, BusNumber);
AK_TYPEDEF_DISTINCT_ORDERED_ID(u8, DeviceNumber);
AK_TYPEDEF_DISTINCT_ORDERED_ID(u8, FunctionNumber);

struct PCIInterruptSpecifier {
    u8 interrupt_pin { 0 };
    FunctionNumber function { 0 };
    DeviceNumber device { 0 };
    BusNumber bus { 0 };

    bool operator==(PCIInterruptSpecifier const& other) const
    {
        return bus == other.bus && device == other.device && function == other.function && interrupt_pin == other.interrupt_pin;
    }
    PCIInterruptSpecifier operator&(PCIInterruptSpecifier other) const
    {
        return PCIInterruptSpecifier {
            .interrupt_pin = static_cast<u8>(interrupt_pin & other.interrupt_pin),
            .function = function.value() & other.function.value(),
            .device = device.value() & other.device.value(),
            .bus = bus.value() & other.bus.value(),
        };
    }

    PCIInterruptSpecifier& operator&=(PCIInterruptSpecifier const& other)
    {
        *this = *this & other;
        return *this;
    }
};

}
namespace AK {
template<>
struct Traits<Kernel::PCI::PCIInterruptSpecifier> : public DefaultTraits<Kernel::PCI::PCIInterruptSpecifier> {
    static unsigned hash(Kernel::PCI::PCIInterruptSpecifier value)
    {
        return int_hash(value.bus.value() << 24 | value.device.value() << 16 | value.function.value() << 8 | value.interrupt_pin);
    }
};

}
namespace Kernel::PCI {

struct PCIConfiguration {
    FlatPtr mmio_32bit_base { 0 };
    FlatPtr mmio_32bit_end { 0 };
    FlatPtr mmio_64bit_base { 0 };
    FlatPtr mmio_64bit_end { 0 };
    // The keys contains the bus, device & function at the same offsets as OpenFirmware PCI addresses,
    // with the least significant 8 bits being the interrupt pin.
    HashMap<PCIInterruptSpecifier, u64> masked_interrupt_mapping;
    PCIInterruptSpecifier interrupt_mask;
};

class HostController {
public:
    virtual ~HostController() = default;

    void write8_field(BusNumber, DeviceNumber, FunctionNumber, u32 field, u8 value);
    void write16_field(BusNumber, DeviceNumber, FunctionNumber, u32 field, u16 value);
    void write32_field(BusNumber, DeviceNumber, FunctionNumber, u32 field, u32 value);

    u8 read8_field(BusNumber, DeviceNumber, FunctionNumber, u32 field);
    u16 read16_field(BusNumber, DeviceNumber, FunctionNumber, u32 field);
    u32 read32_field(BusNumber, DeviceNumber, FunctionNumber, u32 field);

    u32 domain_number() const { return m_domain.domain_number(); }

    void enumerate_attached_devices(Function<void(EnumerableDeviceIdentifier const&)> callback, Function<void(EnumerableDeviceIdentifier const&)> post_bridge_callback = nullptr);
    void configure_attached_devices(PCIConfiguration&);

private:
    void enumerate_bus(Function<void(EnumerableDeviceIdentifier const&)> const& callback, Function<void(EnumerableDeviceIdentifier const&)>& post_bridge_callback, BusNumber, bool recursive_search_into_bridges);
    void enumerate_functions(Function<void(EnumerableDeviceIdentifier const&)> const& callback, Function<void(EnumerableDeviceIdentifier const&)>& post_bridge_callback, BusNumber, DeviceNumber, FunctionNumber, bool recursive_search_into_bridges);
    void enumerate_device(Function<void(EnumerableDeviceIdentifier const&)> const& callback, Function<void(EnumerableDeviceIdentifier const&)>& post_bridge_callback, BusNumber bus, DeviceNumber device, bool recursive_search_into_bridges);

    void write8_field(BusNumber, DeviceNumber, FunctionNumber, RegisterOffset field, u8 value);
    void write16_field(BusNumber, DeviceNumber, FunctionNumber, RegisterOffset field, u16 value);
    void write32_field(BusNumber, DeviceNumber, FunctionNumber, RegisterOffset field, u32 value);

    u8 read8_field(BusNumber, DeviceNumber, FunctionNumber, RegisterOffset field);
    u16 read16_field(BusNumber, DeviceNumber, FunctionNumber, RegisterOffset field);

    Optional<u8> get_capabilities_pointer_for_function(BusNumber, DeviceNumber, FunctionNumber);
    Vector<Capability> get_capabilities_for_function(BusNumber, DeviceNumber, FunctionNumber);

protected:
    virtual void write8_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field, u8 value) = 0;
    virtual void write16_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field, u16 value) = 0;
    virtual void write32_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field, u32 value) = 0;

    virtual u8 read8_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field) = 0;
    virtual u16 read16_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field) = 0;
    virtual u32 read32_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field) = 0;

    explicit HostController(PCI::Domain const& domain);

    const PCI::Domain m_domain;

    Spinlock<LockRank::None> m_access_lock;

private:
    Bitmap m_enumerated_buses;
};

}
