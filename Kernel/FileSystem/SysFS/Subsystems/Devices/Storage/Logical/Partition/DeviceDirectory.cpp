/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Logical/Partition/DeviceAttribute.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Logical/Partition/DeviceDirectory.h>
#include <Kernel/Sections.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

DiskPartition const& PartitionDeviceSysFSDirectory::device(Badge<PartitionDeviceAttributeSysFSComponent>) const
{
    return *m_device;
}

NonnullRefPtr<PartitionDeviceSysFSDirectory> PartitionDeviceSysFSDirectory::create(SysFSDirectory const& parent_directory, DiskPartition const& device)
{
    // FIXME: Handle allocation failure gracefully
    auto device_name = MUST(KString::formatted("{}", device.minor().value()));
    auto directory = adopt_ref(*new (nothrow) PartitionDeviceSysFSDirectory(move(device_name), parent_directory, device));
    MUST(directory->m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(PartitionDeviceAttributeSysFSComponent::must_create(*directory, PartitionDeviceAttributeSysFSComponent::Type::StartLBA));
        list.append(PartitionDeviceAttributeSysFSComponent::must_create(*directory, PartitionDeviceAttributeSysFSComponent::Type::EndLBA));
        if (!device.metadata().unique_guid().is_zero())
            list.append(PartitionDeviceAttributeSysFSComponent::must_create(*directory, PartitionDeviceAttributeSysFSComponent::Type::UUID));
        list.append(PartitionDeviceAttributeSysFSComponent::must_create(*directory, PartitionDeviceAttributeSysFSComponent::Type::PartitionType));
        list.append(PartitionDeviceAttributeSysFSComponent::must_create(*directory, PartitionDeviceAttributeSysFSComponent::Type::Attributes));
        return {};
    }));
    return directory;
}

PartitionDeviceSysFSDirectory::PartitionDeviceSysFSDirectory(NonnullOwnPtr<KString> device_directory_name, SysFSDirectory const& parent_directory, DiskPartition const& device)
    : SysFSDirectory(parent_directory)
    , m_device(device)
    , m_device_directory_name(move(device_directory_name))
{
}

}
