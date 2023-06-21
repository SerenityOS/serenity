/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/DeviceDirectory.h>
#include <Kernel/Library/KBuffer.h>

namespace Kernel {

class StorageDeviceAttributeSysFSComponent : public SysFSComponent {
public:
    enum class Type {
        EndLBA,
        SectorSize,
        CommandSet,
    };

public:
    static NonnullRefPtr<StorageDeviceAttributeSysFSComponent> must_create(StorageDeviceSysFSDirectory const& device_directory, Type);

    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const override;
    virtual ~StorageDeviceAttributeSysFSComponent() {};

    virtual StringView name() const override;

protected:
    ErrorOr<NonnullOwnPtr<KBuffer>> try_to_generate_buffer() const;
    StorageDeviceAttributeSysFSComponent(StorageDeviceSysFSDirectory const& device, Type);
    NonnullRefPtr<StorageDevice> m_device;
    Type const m_type { Type::EndLBA };
};

}
