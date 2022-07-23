/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/KString.h>

namespace Kernel {

class StorageDeviceSysFSDirectory;
class PartitionDeviceSymbolicLinkSysFSComponent;
class StorageDevice;
class StorageDevicePartitionsSysFSDirectory final : public SysFSDirectory {
public:
    static NonnullRefPtr<StorageDevicePartitionsSysFSDirectory> must_create(StorageDeviceSysFSDirectory const&);

    virtual StringView name() const override { return "partitions"sv; }

    void plug(Badge<StorageDevice>, PartitionDeviceSymbolicLinkSysFSComponent&);
    void clear(Badge<StorageDevice>);

private:
    explicit StorageDevicePartitionsSysFSDirectory(StorageDeviceSysFSDirectory const&);
};

}
