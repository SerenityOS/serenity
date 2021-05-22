/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Types.h>
#include <Kernel/Graphics/Console/Console.h>
#include <Kernel/Graphics/FramebufferDevice.h>
#include <Kernel/Graphics/GraphicsDevice.h>
#include <Kernel/PCI/DeviceController.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class VGACompatibleAdapter : public GraphicsDevice
    , public PCI::DeviceController {
    AK_MAKE_ETERNAL
public:
    static NonnullRefPtr<VGACompatibleAdapter> initialize_with_preset_resolution(PCI::Address, PhysicalAddress, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch);
    static NonnullRefPtr<VGACompatibleAdapter> initialize(PCI::Address);

    virtual bool framebuffer_devices_initialized() const override { return !m_framebuffer_device.is_null(); }

    virtual bool modesetting_capable() const override { return false; }
    virtual bool double_framebuffering_capable() const override { return false; }

    virtual bool try_to_set_resolution(size_t output_port_index, size_t width, size_t height) override;
    virtual bool set_y_offset(size_t output_port_index, size_t y) override;

protected:
    explicit VGACompatibleAdapter(PCI::Address);

private:
    VGACompatibleAdapter(PCI::Address, PhysicalAddress, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch);

    // ^GraphicsDevice
    virtual void initialize_framebuffer_devices() override;
    virtual Type type() const override { return Type::VGACompatible; }

    virtual void enable_consoles() override;
    virtual void disable_consoles() override;

protected:
    PhysicalAddress m_framebuffer_address;
    size_t m_framebuffer_width { 0 };
    size_t m_framebuffer_height { 0 };
    size_t m_framebuffer_pitch { 0 };

    RefPtr<FramebufferDevice> m_framebuffer_device;
    RefPtr<Graphics::Console> m_framebuffer_console;
};

}
