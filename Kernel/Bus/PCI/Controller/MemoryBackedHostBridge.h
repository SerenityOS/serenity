/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/Vector.h>
#include <Kernel/Bus/PCI/Controller/HostController.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/Region.h>

namespace Kernel::PCI {

class MemoryBackedHostBridge : public HostController {
public:
    static NonnullOwnPtr<MemoryBackedHostBridge> must_create(Domain const&, PhysicalAddress);

protected:
    virtual void write8_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field, u8 value) override;
    virtual void write16_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field, u16 value) override;
    virtual void write32_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field, u32 value) override;

    virtual u8 read8_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field) override;
    virtual u16 read16_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field) override;
    virtual u32 read32_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field) override;

    MemoryBackedHostBridge(PCI::Domain const&, PhysicalAddress);

    // Memory-mapped access operations
    void map_bus_region(BusNumber);
    VirtualAddress get_device_configuration_memory_mapped_space(BusNumber, DeviceNumber, FunctionNumber);
    PhysicalAddress determine_memory_mapped_bus_base_address(BusNumber) const;

    // Data-members for accessing Memory mapped PCI devices' configuration spaces
    BusNumber m_mapped_bus { 0 };
    OwnPtr<Memory::Region> m_mapped_bus_region;
    PhysicalAddress m_start_address;
};

}
