/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Graphics/Console/Console.h>
#include <Kernel/Graphics/GenericGraphicsAdapter.h>
#include <Kernel/Graphics/VGA/GenericAdapter.h>
#include <Kernel/Graphics/VGA/GenericDisplayConnector.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class PCIVGAGenericAdapter : public VGAGenericAdapter
    , public PCI::Device {
public:
    static NonnullRefPtr<PCIVGAGenericAdapter> must_create_with_preset_resolution(PCI::DeviceIdentifier const&, PhysicalAddress, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch);
    static NonnullRefPtr<PCIVGAGenericAdapter> must_create(PCI::DeviceIdentifier const&);

protected:
    explicit PCIVGAGenericAdapter(PCI::Address);

    // Note: This is only used in PCIVGAGenericAdapter code because we need
    // to remember how to access the framebuffer
    Optional<PhysicalAddress> m_framebuffer_address;
};
}
