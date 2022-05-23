/*
 * Copyright (c) 2021-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Graphics/Console/Console.h>
#include <Kernel/Graphics/GenericGraphicsAdapter.h>
#include <Kernel/Graphics/VGA/DisplayConnector.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class VGACompatibleAdapter : public GenericGraphicsAdapter {
public:
    virtual bool vga_compatible() const override final { return true; }

protected:
    void initialize_display_connector_with_preset_resolution(PhysicalAddress, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch);

    VGACompatibleAdapter() = default;

    RefPtr<GenericDisplayConnector> m_generic_display_connector;
};
}
