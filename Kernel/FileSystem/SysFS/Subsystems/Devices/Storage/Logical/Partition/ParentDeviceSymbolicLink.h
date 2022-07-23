/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Logical/Partition/DeviceDirectory.h>

namespace Kernel {

class PartitionDeviceParentDeviceSymbolicLinkSysFSComponent final
    : public SysFSSymbolicLink
    , public Weakable<PartitionDeviceParentDeviceSymbolicLinkSysFSComponent> {
    friend class SysFSComponentRegistry;

public:
    static ErrorOr<NonnullRefPtr<PartitionDeviceParentDeviceSymbolicLinkSysFSComponent>> try_create(PartitionDeviceSysFSDirectory const& parent_directory, SysFSComponent const& pointed_component);

    virtual StringView name() const override { return "parent_device"sv; }

private:
    PartitionDeviceParentDeviceSymbolicLinkSysFSComponent(PartitionDeviceSysFSDirectory const& parent_directory, SysFSComponent const& pointed_component);
};

}
