/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/Vector.h>
#include <Kernel/Bus/PCI/Controller/MemoryBackedHostBridge.h>
#include <Kernel/Library/Driver.h>
#include <Kernel/Locking/Spinlock.h>

namespace Kernel::PCI {

class VolumeManagementDevice final
    : public MemoryBackedHostBridge {
    KERNEL_MAKE_DRIVER_LISTABLE(VolumeManagementDevice)

public:
    static ErrorOr<NonnullRefPtr<VolumeManagementDevice>> create(PCI::Device& device, NonnullRefPtr<Bus> parent_bus);

private:
    VolumeManagementDevice(PCI::Domain const&, NonnullRefPtr<Bus> root_bus, PhysicalAddress);

    virtual void write8_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field, u8 value) override;
    virtual void write16_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field, u16 value) override;
    virtual void write32_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field, u32 value) override;
    virtual u8 read8_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field) override;
    virtual u16 read16_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field) override;
    virtual u32 read32_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field) override;

    // Note: All read and writes must be done with a spinlock because
    // Linux says that CPU might deadlock otherwise if access is not serialized.
    Spinlock<LockRank::None> m_config_lock {};
};

}
