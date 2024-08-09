/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Devices/GPU/Bochs/Definitions.h>
#include <Kernel/Devices/GPU/Console/GenericFramebufferConsole.h>
#include <Kernel/Devices/GPU/GPUDevice.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

class GraphicsManagement;
struct BochsDisplayMMIORegisters;

class BochsGraphicsAdapter final : public GPUDevice
    , public PCI::Device {
    friend class GraphicsManagement;

public:
    static ErrorOr<bool> probe(PCI::DeviceIdentifier const&);
    static ErrorOr<NonnullLockRefPtr<GPUDevice>> create(PCI::DeviceIdentifier const&);
    virtual ~BochsGraphicsAdapter() = default;
    virtual StringView device_name() const override { return "BochsGraphicsAdapter"sv; }

private:
    ErrorOr<void> initialize_adapter(PCI::DeviceIdentifier const&);

    explicit BochsGraphicsAdapter(PCI::DeviceIdentifier const&);

    RefPtr<DisplayConnector> m_display_connector;
};
}
