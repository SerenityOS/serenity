/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Devices/GPU/Definitions.h>
#include <Kernel/Devices/GPU/Intel/DisplayConnectorGroup.h>
#include <Kernel/Devices/GPU/Intel/NativeDisplayConnector.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <LibEDID/EDID.h>

namespace Kernel {

class IntelNativeGraphicsAdapter final
    : public GPUDevice
    , public PCI::Device {

public:
    static ErrorOr<bool> probe(PCI::DeviceIdentifier const&);
    static ErrorOr<NonnullLockRefPtr<GPUDevice>> create(PCI::DeviceIdentifier const&);

    virtual ~IntelNativeGraphicsAdapter() = default;

    virtual StringView device_name() const override { return "IntelNativeGraphicsAdapter"sv; }

private:
    ErrorOr<void> initialize_adapter();

    explicit IntelNativeGraphicsAdapter(PCI::DeviceIdentifier const&);

    LockRefPtr<IntelDisplayConnectorGroup> m_connector_group;
};
}
