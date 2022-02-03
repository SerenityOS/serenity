/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Graphics/Definitions.h>
#include <Kernel/Graphics/FramebufferDevice.h>
#include <Kernel/Graphics/Intel/NativeDisplayConnector.h>
#include <Kernel/Graphics/VGA/PCIGenericAdapter.h>
#include <Kernel/PhysicalAddress.h>
#include <LibEDID/EDID.h>

namespace Kernel {

class IntelNativeGraphicsAdapter final
    : public PCIVGAGenericAdapter {

public:
    static RefPtr<IntelNativeGraphicsAdapter> initialize(PCI::DeviceIdentifier const&);

private:
    ErrorOr<void> initialize_adapter();

    explicit IntelNativeGraphicsAdapter(PCI::Address);

    // ^GenericGraphicsAdapter
    virtual void enable_consoles() override;
    virtual void disable_consoles() override;

    // ^GenericGraphicsAdapter
    virtual void initialize_framebuffer_devices() override;
    virtual ErrorOr<ByteBuffer> get_edid(size_t output_port_index) const override;
};
}
