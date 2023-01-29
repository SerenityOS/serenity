/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/Vector.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Locking/Spinlock.h>

namespace Kernel::PCI {

AK_TYPEDEF_DISTINCT_ORDERED_ID(u8, BusNumber);
AK_TYPEDEF_DISTINCT_ORDERED_ID(u8, DeviceNumber);
AK_TYPEDEF_DISTINCT_ORDERED_ID(u8, FunctionNumber);

class HostController {
public:
    virtual ~HostController() = default;

    virtual void write8_field(BusNumber, DeviceNumber, FunctionNumber, u32 field, u8 value) = 0;
    virtual void write16_field(BusNumber, DeviceNumber, FunctionNumber, u32 field, u16 value) = 0;
    virtual void write32_field(BusNumber, DeviceNumber, FunctionNumber, u32 field, u32 value) = 0;

    virtual u8 read8_field(BusNumber, DeviceNumber, FunctionNumber, u32 field) = 0;
    virtual u16 read16_field(BusNumber, DeviceNumber, FunctionNumber, u32 field) = 0;
    virtual u32 read32_field(BusNumber, DeviceNumber, FunctionNumber, u32 field) = 0;

    u32 domain_number() const { return m_domain.domain_number(); }

    void enumerate_attached_devices(Function<IterationDecision(EnumerableDeviceIdentifier)> callback);

private:
    void enumerate_bus(Function<IterationDecision(EnumerableDeviceIdentifier)> const& callback, BusNumber, bool recursive);
    void enumerate_functions(Function<IterationDecision(EnumerableDeviceIdentifier)> const& callback, BusNumber, DeviceNumber, FunctionNumber, bool recursive);
    void enumerate_device(Function<IterationDecision(EnumerableDeviceIdentifier)> const& callback, BusNumber bus, DeviceNumber device, bool recursive);

    u8 read8_field(BusNumber, DeviceNumber, FunctionNumber, RegisterOffset field);
    u16 read16_field(BusNumber, DeviceNumber, FunctionNumber, RegisterOffset field);

    Optional<u8> get_capabilities_pointer_for_function(BusNumber, DeviceNumber, FunctionNumber);
    Vector<Capability> get_capabilities_for_function(BusNumber, DeviceNumber, FunctionNumber);

protected:
    explicit HostController(PCI::Domain const& domain);

    const PCI::Domain m_domain;

private:
    Bitmap m_enumerated_buses;
};

}
