/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <Kernel/Bus/PCI/Controller/HostController.h>
#include <Kernel/Locking/Spinlock.h>

namespace Kernel::PCI {

class HostBridge : public HostController {
public:
    static NonnullOwnPtr<HostBridge> must_create_with_io_access();

    virtual void write8_field(BusNumber, DeviceNumber, FunctionNumber, u32 field, u8 value) override;
    virtual void write16_field(BusNumber, DeviceNumber, FunctionNumber, u32 field, u16 value) override;
    virtual void write32_field(BusNumber, DeviceNumber, FunctionNumber, u32 field, u32 value) override;

    virtual u8 read8_field(BusNumber, DeviceNumber, FunctionNumber, u32 field) override;
    virtual u16 read16_field(BusNumber, DeviceNumber, FunctionNumber, u32 field) override;
    virtual u32 read32_field(BusNumber, DeviceNumber, FunctionNumber, u32 field) override;

protected:
    explicit HostBridge(PCI::Domain const&);

private:
    virtual void enumerate_attached_devices(Function<void(DeviceIdentifier)> callback) override;

    Bitmap m_enumerated_buses;

    u8 read8_field(BusNumber, DeviceNumber, FunctionNumber, RegisterOffset field);
    u16 read16_field(BusNumber, DeviceNumber, FunctionNumber, RegisterOffset field);

    Optional<u8> get_capabilities_pointer_for_function(BusNumber, DeviceNumber, FunctionNumber);
    Vector<Capability> get_capabilities_for_function(BusNumber, DeviceNumber, FunctionNumber);

    void enumerate_bus(Function<void(DeviceIdentifier)> const& callback, BusNumber, bool recursive);
    void enumerate_functions(Function<void(DeviceIdentifier)> const& callback, BusNumber, DeviceNumber, FunctionNumber, bool recursive);
    void enumerate_device(Function<void(DeviceIdentifier)> const& callback, BusNumber bus, DeviceNumber device, bool recursive);
};

}
