/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Devices/Storage/StorageDevice.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/DeviceAttribute.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/DeviceDirectory.h>
#include <Kernel/Sections.h>

namespace Kernel {

StorageDevice const& StorageDeviceSysFSDirectory::device(Badge<StorageDeviceAttributeSysFSComponent>) const
{
    return *m_device;
}

NonnullRefPtr<StorageDeviceSysFSDirectory> StorageDeviceSysFSDirectory::create(SysFSDirectory const& parent_directory, StorageDevice const& device)
{
    // FIXME: Handle allocation failure gracefully
    auto lun_address = device.logical_unit_number_address();
    auto device_name = MUST(KString::formatted("{:02x}:{:02x}.{}", lun_address.controller_id, lun_address.target_id, lun_address.disk_id));
    auto directory = adopt_ref(*new (nothrow) StorageDeviceSysFSDirectory(move(device_name), parent_directory, device));
    MUST(directory->m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(StorageDeviceAttributeSysFSComponent::must_create(*directory, StorageDeviceAttributeSysFSComponent::Type::EndLBA));
        list.append(StorageDeviceAttributeSysFSComponent::must_create(*directory, StorageDeviceAttributeSysFSComponent::Type::SectorSize));
        list.append(StorageDeviceAttributeSysFSComponent::must_create(*directory, StorageDeviceAttributeSysFSComponent::Type::CommandSet));
        return {};
    }));
    return directory;
}

StorageDeviceSysFSDirectory::StorageDeviceSysFSDirectory(NonnullOwnPtr<KString> device_directory_name, SysFSDirectory const& parent_directory, StorageDevice const& device)
    : SysFSDirectory(parent_directory)
    , m_device(device)
    , m_device_directory_name(move(device_directory_name))
{
}

}
