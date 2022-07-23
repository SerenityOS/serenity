/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Logical/Partition/DeviceDirectory.h>

namespace Kernel {

class StorageDevicePartitionsSysFSDirectory;
class PartitionDeviceSymbolicLinkSysFSComponent final
    : public SysFSSymbolicLink
    , public Weakable<PartitionDeviceSymbolicLinkSysFSComponent> {
    friend class SysFSComponentRegistry;

public:
    static ErrorOr<NonnullRefPtr<PartitionDeviceSymbolicLinkSysFSComponent>> try_create(StorageDevicePartitionsSysFSDirectory const& parent_directory, size_t partition_index, SysFSComponent const& pointed_component);

    virtual StringView name() const override { return m_symlink_name->view(); }

private:
    PartitionDeviceSymbolicLinkSysFSComponent(NonnullOwnPtr<KString> symlink_name, StorageDevicePartitionsSysFSDirectory const& parent_directory, SysFSComponent const& pointed_component);
    NonnullOwnPtr<KString> m_symlink_name;
};

}
