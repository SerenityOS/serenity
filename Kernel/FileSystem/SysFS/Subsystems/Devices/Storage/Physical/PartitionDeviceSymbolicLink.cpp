/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Device.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Physical/PartitionDeviceSymbolicLink.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Physical/PartitionsDirectory.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<PartitionDeviceSymbolicLinkSysFSComponent>> PartitionDeviceSymbolicLinkSysFSComponent::try_create(StorageDevicePartitionsSysFSDirectory const& parent_directory, size_t partition_index, SysFSComponent const& pointed_component)
{
    auto node_name = TRY(KString::formatted("{}", partition_index));
    return adopt_nonnull_ref_or_enomem(new (nothrow) PartitionDeviceSymbolicLinkSysFSComponent(move(node_name), parent_directory, pointed_component));
}

PartitionDeviceSymbolicLinkSysFSComponent::PartitionDeviceSymbolicLinkSysFSComponent(NonnullOwnPtr<KString> symlink_name, StorageDevicePartitionsSysFSDirectory const& parent_directory, SysFSComponent const& pointed_component)
    : SysFSSymbolicLink(parent_directory, pointed_component)
    , m_symlink_name(move(symlink_name))
{
}

}
