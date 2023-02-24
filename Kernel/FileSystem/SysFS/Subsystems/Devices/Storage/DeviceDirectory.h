/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/Storage/StorageDevice.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/Library/KString.h>

namespace Kernel {

class StorageDeviceAttributeSysFSComponent;
class StorageDeviceSysFSDirectory final : public SysFSDirectory {
public:
    static NonnullRefPtr<StorageDeviceSysFSDirectory> create(SysFSDirectory const&, StorageDevice const&);

    virtual StringView name() const override { return m_device_directory_name->view(); }

    StorageDevice const& device(Badge<StorageDeviceAttributeSysFSComponent>) const;

private:
    StorageDeviceSysFSDirectory(NonnullOwnPtr<KString> device_directory_name, SysFSDirectory const&, StorageDevice const&);
    RefPtr<StorageDevice> m_device;
    NonnullOwnPtr<KString> m_device_directory_name;
};

}
