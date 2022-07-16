/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Graphics/GenericGraphicsAdapter.h>

namespace Kernel {
class PCIGraphicsAdapter
    : public GenericGraphicsAdapter
    , public PCI::Device {
    friend class GraphicsManagement;

public:
    virtual ~PCIGraphicsAdapter() = default;

protected:
    explicit PCIGraphicsAdapter(PCI::DeviceIdentifier const& pci_device_identifier);
};

}
