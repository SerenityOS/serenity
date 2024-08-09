/*
 * Copyright (c) 2023, Edwin Rijkee <edwin@virtualparadise.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Devices/GPU/Console/GenericFramebufferConsole.h>
#include <Kernel/Devices/GPU/GPUDevice.h>

namespace Kernel {

class VoodooGraphicsAdapter final : public GPUDevice
    , public PCI::Device {

public:
    static ErrorOr<bool> probe(PCI::DeviceIdentifier const&);
    static ErrorOr<NonnullLockRefPtr<GPUDevice>> create(PCI::DeviceIdentifier const&);
    virtual ~VoodooGraphicsAdapter() = default;
    virtual StringView device_name() const override { return "VoodooGraphicsAdapter"sv; }

private:
    ErrorOr<void> initialize_adapter(PCI::DeviceIdentifier const&);

    explicit VoodooGraphicsAdapter(PCI::DeviceIdentifier const&);

    RefPtr<DisplayConnector> m_display_connector;
};
}
