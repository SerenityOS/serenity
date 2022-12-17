/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Graphics/Bochs/Definitions.h>
#include <Kernel/Graphics/Console/GenericFramebufferConsole.h>
#include <Kernel/Graphics/GenericGraphicsAdapter.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class GraphicsManagement;
struct BochsDisplayMMIORegisters;

class BochsGraphicsAdapter final : public GenericGraphicsAdapter
    , public PCI::Device {
    friend class GraphicsManagement;

public:
    static ErrorOr<bool> probe(PCI::DeviceIdentifier const&);
    static ErrorOr<NonnullLockRefPtr<GenericGraphicsAdapter>> create(PCI::DeviceIdentifier const&);
    virtual ~BochsGraphicsAdapter() = default;
    virtual StringView device_name() const override { return "BochsGraphicsAdapter"sv; }

private:
    ErrorOr<void> initialize_adapter(PCI::DeviceIdentifier const&);

    explicit BochsGraphicsAdapter(PCI::Address const&);

    LockRefPtr<DisplayConnector> m_display_connector;
};
}
