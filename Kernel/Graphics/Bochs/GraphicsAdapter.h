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

class BochsDisplayConnector;
class BochsGraphicsAdapter final : public GenericGraphicsAdapter
    , public PCI::Device {
    friend class GraphicsManagement;

public:
    static NonnullLockRefPtr<BochsGraphicsAdapter> initialize(PCI::DeviceIdentifier const&);
    virtual ~BochsGraphicsAdapter() = default;

private:
    ErrorOr<void> initialize_adapter(PCI::DeviceIdentifier const&);

    explicit BochsGraphicsAdapter(PCI::DeviceIdentifier const&);

    LockRefPtr<BochsDisplayConnector> m_display_connector;
};
}
