/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/Forward.h>

namespace Kernel {

class StorageDeviceSysFSDirectory;
class StorageDevice;
class SysFSStorageDirectory;
class SysFSStoragePhysicalDevicesDirectory : public SysFSDirectory {
    friend class SysFSComponentRegistry;

public:
    virtual StringView name() const override { return "physical"sv; }
    static SysFSStoragePhysicalDevicesDirectory& the();
    static NonnullRefPtr<SysFSStoragePhysicalDevicesDirectory> must_create(SysFSStorageDirectory const&);

    void plug(Badge<StorageDevice>, StorageDeviceSysFSDirectory&);
    void unplug(Badge<StorageDevice>, SysFSDirectory&);

private:
    explicit SysFSStoragePhysicalDevicesDirectory(SysFSStorageDirectory const&);
};

}
