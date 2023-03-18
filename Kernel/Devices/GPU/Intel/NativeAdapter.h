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
#include <Kernel/PhysicalAddress.h>
#include <LibEDID/EDID.h>

namespace Kernel {

class IntelNativeGPUAdapter final
    : public GenericGPUAdapter
    , public PCI::Device {

public:
    static ErrorOr<bool> probe(PCI::DeviceIdentifier const&);
    static ErrorOr<NonnullLockRefPtr<GenericGPUAdapter>> create(PCI::DeviceIdentifier const&);

    virtual ~IntelNativeGPUAdapter() = default;

    virtual StringView device_name() const override { return "IntelNativeGPUAdapter"sv; }

private:
    ErrorOr<void> initialize_adapter();

    explicit IntelNativeGPUAdapter(PCI::DeviceIdentifier const&);

    LockRefPtr<IntelDisplayConnectorGroup> m_connector_group;
};
}
