/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Controller/VolumeManagementDevice.h>

namespace Kernel::PCI {

static Atomic<u32> s_vmd_pci_domain_number = 0x10000;

ErrorOr<NonnullRefPtr<VolumeManagementDevice>> VolumeManagementDevice::create(PCI::Device& device, NonnullRefPtr<Bus> parent_bus)
{
    SpinlockLocker locker(device.operation_lock());

    u8 start_bus = 0;
    auto start_bus_identifier = (device.config_space_read16(static_cast<PCI::RegisterOffset>(0x44)) >> 8) & 0x3;
    switch (start_bus_identifier) {
    case 0:
        break;
    case 1:
        start_bus = 128;
        break;
    case 2:
        start_bus = 224;
        break;
    default:
        dbgln("VMD @ {}: Unknown bus offset option was set to {}", device.device_id().address(), start_bus_identifier);
        VERIFY_NOT_REACHED();
    }

    // FIXME: The end bus might not be 255, so we actually need to check it with the
    // resource size of BAR0.
    dbgln("VMD Host bridge @ {}: Start bus at {}, end bus {}", device.device_id().address(), start_bus, 0xff);
    PCI::Domain domain { s_vmd_pci_domain_number++, start_bus, 0xff };
    auto start_address = PhysicalAddress(device.resources()[0].physical_memory_address()).page_base();
    auto root_bus = TRY(Bus::create(domain.start_bus(), device, parent_bus));
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) VolumeManagementDevice(domain, root_bus, start_address)));
}

void VolumeManagementDevice::write8_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field, u8 value)
{
    SpinlockLocker locker(m_config_lock);
    // Note: We must write then read to ensure completion before returning.
    MemoryBackedHostBridge::write8_field_locked(bus, device, function, field, value);
    MemoryBackedHostBridge::read8_field_locked(bus, device, function, field);
}
void VolumeManagementDevice::write16_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field, u16 value)
{
    SpinlockLocker locker(m_config_lock);
    // Note: We must write then read to ensure completion before returning.
    MemoryBackedHostBridge::write16_field_locked(bus, device, function, field, value);
    MemoryBackedHostBridge::read16_field_locked(bus, device, function, field);
}
void VolumeManagementDevice::write32_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field, u32 value)
{
    SpinlockLocker locker(m_config_lock);
    // Note: We must write then read to ensure completion before returning.
    MemoryBackedHostBridge::write32_field_locked(bus, device, function, field, value);
    MemoryBackedHostBridge::read32_field_locked(bus, device, function, field);
}

u8 VolumeManagementDevice::read8_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field)
{
    SpinlockLocker locker(m_config_lock);
    return MemoryBackedHostBridge::read8_field_locked(bus, device, function, field);
}
u16 VolumeManagementDevice::read16_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field)
{
    SpinlockLocker locker(m_config_lock);
    return MemoryBackedHostBridge::read16_field_locked(bus, device, function, field);
}
u32 VolumeManagementDevice::read32_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field)
{
    SpinlockLocker locker(m_config_lock);
    return MemoryBackedHostBridge::read32_field_locked(bus, device, function, field);
}

VolumeManagementDevice::VolumeManagementDevice(PCI::Domain const& domain, NonnullRefPtr<Bus> root_bus, PhysicalAddress start_address)
    : MemoryBackedHostBridge(domain, root_bus, start_address)
{
}

}
