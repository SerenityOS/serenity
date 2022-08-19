/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/KString.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

class StorageDeviceAttributeSysFSComponent;
class StorageDeviceSysFSDirectory final : public SysFSDirectory {
public:
    static NonnullLockRefPtr<StorageDeviceSysFSDirectory> create(SysFSDirectory const&, StorageDevice const&);

    virtual StringView name() const override { return m_device_directory_name->view(); }

    StorageDevice const& device(Badge<StorageDeviceAttributeSysFSComponent>) const;

private:
    StorageDeviceSysFSDirectory(NonnullOwnPtr<KString> device_directory_name, SysFSDirectory const&, StorageDevice const&);
    LockRefPtr<StorageDevice> m_device;
    NonnullOwnPtr<KString> m_device_directory_name;
};

}
