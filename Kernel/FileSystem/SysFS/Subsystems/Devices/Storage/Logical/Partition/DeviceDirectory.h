/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/KString.h>
#include <Kernel/Storage/DiskPartition.h>

namespace Kernel {

class PartitionDeviceAttributeSysFSComponent;
class PartitionDeviceSysFSDirectory final : public SysFSDirectory {
public:
    static NonnullRefPtr<PartitionDeviceSysFSDirectory> create(SysFSDirectory const&, DiskPartition const&);

    virtual StringView name() const override { return m_device_directory_name->view(); }

    DiskPartition const& device(Badge<PartitionDeviceAttributeSysFSComponent>) const;

private:
    PartitionDeviceSysFSDirectory(NonnullOwnPtr<KString> device_directory_name, SysFSDirectory const&, DiskPartition const&);
    RefPtr<DiskPartition> m_device;
    NonnullOwnPtr<KString> m_device_directory_name;
};

}
