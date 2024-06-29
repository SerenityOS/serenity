/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/API/DeviceFileTypes.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/GlobalInformation.h>
#include <Kernel/Library/KBufferBuilder.h>
#include <Kernel/Library/UserOrKernelBuffer.h>

namespace Kernel {

class SysFSDeviceMajorNumberAllocations final : public SysFSGlobalInformation {
public:
    virtual StringView name() const override;

    static NonnullRefPtr<SysFSDeviceMajorNumberAllocations> must_create(SysFSDirectory const& parent_directory, DeviceNodeType);

private:
    explicit SysFSDeviceMajorNumberAllocations(SysFSDirectory const& parent_directory, DeviceNodeType);
    virtual ErrorOr<void> try_generate(KBufferBuilder& builder) override;

    DeviceNodeType m_device_node_type { DeviceNodeType::Block };
};

}
