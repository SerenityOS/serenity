/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Controller/VolumeManagementDevice.h>

namespace Kernel::PCI {

static Atomic<u32> s_vmd_pci_domain_number = 0x10000;

NonnullOwnPtr<VolumeManagementDevice> VolumeManagementDevice::must_create(PCI::DeviceIdentifier const& device_identifier)
{
    u8 start_bus = 0;
    switch ((PCI::read16(device_identifier.address(), static_cast<PCI::RegisterOffset>(0x44)) >> 8) & 0x3) {
    case 0:
        break;
    case 1:
        start_bus = 128;
        break;
    case 2:
        start_bus = 224;
        break;
    default:
        dbgln("VMD @ {}: Unknown bus offset option was set to {}", device_identifier.address(),
            ((PCI::read16(device_identifier.address(), static_cast<PCI::RegisterOffset>(0x44)) >> 8) & 0x3));
        VERIFY_NOT_REACHED();
    }

    // FIXME: The end bus might not be 255, so we actually need to check it with the
    // resource size of BAR0.
    dbgln("VMD Host bridge @ {}: Start bus at {}, end bus {}", device_identifier.address(), start_bus, 0xff);
    PCI::Domain domain { s_vmd_pci_domain_number++, start_bus, 0xff };
    auto start_address = PhysicalAddress(PCI::get_BAR0(device_identifier.address())).page_base();
    return adopt_own_if_nonnull(new (nothrow) VolumeManagementDevice(domain, start_address)).release_nonnull();
}

void VolumeManagementDevice::write8_field(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field, u8 value)
{
    SpinlockLocker locker(m_config_lock);
    // Note: We must write then read to ensure completion before returning.
    MemoryBackedHostBridge::write8_field(bus, device, function, field, value);
    MemoryBackedHostBridge::read8_field(bus, device, function, field);
}
void VolumeManagementDevice::write16_field(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field, u16 value)
{
    SpinlockLocker locker(m_config_lock);
    // Note: We must write then read to ensure completion before returning.
    MemoryBackedHostBridge::write16_field(bus, device, function, field, value);
    MemoryBackedHostBridge::read16_field(bus, device, function, field);
}
void VolumeManagementDevice::write32_field(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field, u32 value)
{
    SpinlockLocker locker(m_config_lock);
    // Note: We must write then read to ensure completion before returning.
    MemoryBackedHostBridge::write32_field(bus, device, function, field, value);
    MemoryBackedHostBridge::read32_field(bus, device, function, field);
}

u8 VolumeManagementDevice::read8_field(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field)
{
    SpinlockLocker locker(m_config_lock);
    return MemoryBackedHostBridge::read8_field(bus, device, function, field);
}
u16 VolumeManagementDevice::read16_field(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field)
{
    SpinlockLocker locker(m_config_lock);
    return MemoryBackedHostBridge::read16_field(bus, device, function, field);
}
u32 VolumeManagementDevice::read32_field(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field)
{
    SpinlockLocker locker(m_config_lock);
    return MemoryBackedHostBridge::read32_field(bus, device, function, field);
}

VolumeManagementDevice::VolumeManagementDevice(PCI::Domain const& domain, PhysicalAddress start_address)
    : MemoryBackedHostBridge(domain, start_address)
{
}

}
