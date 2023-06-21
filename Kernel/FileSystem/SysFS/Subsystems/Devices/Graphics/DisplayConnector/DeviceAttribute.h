/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/GPU/DisplayConnector.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Graphics/DisplayConnector/DeviceDirectory.h>
#include <Kernel/Library/KBuffer.h>

namespace Kernel {

class DisplayConnectorAttributeSysFSComponent : public SysFSComponent {
public:
    enum class Type {
        MutableModeSettingCapable,
        DoubleFrameBufferingCapable,
        FlushSupport,
        PartialFlushSupport,
        RefreshRateSupport,
        EDID,
    };

public:
    static NonnullRefPtr<DisplayConnectorAttributeSysFSComponent> must_create(DisplayConnectorSysFSDirectory const& device_directory, Type);

    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const override;
    virtual ~DisplayConnectorAttributeSysFSComponent() {};

    virtual StringView name() const override;

protected:
    ErrorOr<NonnullOwnPtr<KBuffer>> try_to_generate_buffer() const;
    DisplayConnectorAttributeSysFSComponent(DisplayConnectorSysFSDirectory const& device, Type);
    NonnullRefPtr<DisplayConnector> m_device;
    Type const m_type { Type::MutableModeSettingCapable };
};

}
