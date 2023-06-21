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
class SysFSStorageDirectory : public SysFSDirectory {
    friend class SysFSComponentRegistry;

public:
    virtual StringView name() const override { return "storage"sv; }
    static SysFSStorageDirectory& the();
    static NonnullRefPtr<SysFSStorageDirectory> must_create(SysFSDevicesDirectory const&);

    void plug(Badge<StorageDevice>, StorageDeviceSysFSDirectory&);
    void unplug(Badge<StorageDevice>, SysFSDirectory&);

private:
    explicit SysFSStorageDirectory(SysFSDevicesDirectory const&);
};

}
