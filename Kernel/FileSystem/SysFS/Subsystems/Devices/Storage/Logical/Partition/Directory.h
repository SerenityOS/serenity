/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/Forward.h>

namespace Kernel {

class SysFSStorageLogicalDevicesDirectory;
class DiskPartition;
class PartitionDeviceSysFSDirectory;
class SysFSStoragePartitionDevicesDirectory : public SysFSDirectory {
    friend class SysFSComponentRegistry;

public:
    virtual StringView name() const override { return "partitions"sv; }
    static SysFSStoragePartitionDevicesDirectory& the();
    static NonnullRefPtr<SysFSStoragePartitionDevicesDirectory> must_create(SysFSStorageLogicalDevicesDirectory const&);

    void plug(Badge<DiskPartition>, PartitionDeviceSysFSDirectory&);
    void unplug(Badge<DiskPartition>, SysFSDirectory&);

private:
    explicit SysFSStoragePartitionDevicesDirectory(SysFSStorageLogicalDevicesDirectory const&);
};

}
