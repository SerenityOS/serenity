/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Graphics/Console/Console.h>
#include <Kernel/Graphics/GenericGraphicsAdapter.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class PCIVGACompatibleAdapter : public VGACompatibleAdapter
    , public PCI::Device {
public:
    static NonnullRefPtr<PCIVGACompatibleAdapter> initialize_with_preset_resolution(PCI::DeviceIdentifier const&, PhysicalAddress, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch);
    static NonnullRefPtr<PCIVGACompatibleAdapter> initialize(PCI::DeviceIdentifier const&);

protected:
    explicit PCIVGACompatibleAdapter(PCI::Address);
};
}
