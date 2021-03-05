/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Types.h>
#include <Kernel/Graphics/GraphicsDevice.h>
#include <Kernel/Graphics/RawFramebufferDevice.h>
#include <Kernel/PCI/DeviceController.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class VGACompatibleAdapter final : public GraphicsDevice
    , public PCI::DeviceController {
    AK_MAKE_ETERNAL
public:
    static NonnullRefPtr<VGACompatibleAdapter> initialize_with_preset_resolution(PCI::Address, PhysicalAddress, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch);

protected:
    explicit VGACompatibleAdapter(PCI::Address);

private:
    VGACompatibleAdapter(PCI::Address, PhysicalAddress, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch);

    // ^GraphicsDevice
    virtual void initialize_framebuffer_devices() override;
    virtual Type type() const override { return Type::VGACompatible; }

protected:
    RefPtr<RawFramebufferDevice> m_framebuffer;
};

}
