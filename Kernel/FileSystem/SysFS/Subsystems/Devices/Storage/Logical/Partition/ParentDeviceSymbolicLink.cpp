/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Device.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Logical/Partition/ParentDeviceSymbolicLink.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<PartitionDeviceParentDeviceSymbolicLinkSysFSComponent>> PartitionDeviceParentDeviceSymbolicLinkSysFSComponent::try_create(PartitionDeviceSysFSDirectory const& parent_directory, SysFSComponent const& pointed_component)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) PartitionDeviceParentDeviceSymbolicLinkSysFSComponent(parent_directory, pointed_component));
}

PartitionDeviceParentDeviceSymbolicLinkSysFSComponent::PartitionDeviceParentDeviceSymbolicLinkSysFSComponent(PartitionDeviceSysFSDirectory const& parent_directory, SysFSComponent const& pointed_component)
    : SysFSSymbolicLink(parent_directory, pointed_component)
{
}

}
