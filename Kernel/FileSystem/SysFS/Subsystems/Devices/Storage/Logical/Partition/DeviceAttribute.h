/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Logical/Partition/DeviceDirectory.h>
#include <Kernel/KBuffer.h>

namespace Kernel {

class PartitionDeviceAttributeSysFSComponent : public SysFSComponent {
public:
    enum class Type {
        StartLBA,
        EndLBA,
        UUID,
        PartitionType,
        Attributes,
    };

public:
    static NonnullRefPtr<PartitionDeviceAttributeSysFSComponent> must_create(PartitionDeviceSysFSDirectory const& device_directory, Type);

    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const override;
    virtual ~PartitionDeviceAttributeSysFSComponent() {};

    virtual StringView name() const override;

protected:
    ErrorOr<NonnullOwnPtr<KBuffer>> try_to_generate_buffer() const;
    PartitionDeviceAttributeSysFSComponent(PartitionDeviceSysFSDirectory const& device, Type);
    NonnullRefPtr<DiskPartition> m_device;
    Type const m_type { Type::EndLBA };
};

}
