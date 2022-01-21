/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Graphics/Console/Console.h>
#include <Kernel/Graphics/FramebufferDevice.h>
#include <Kernel/Graphics/GenericGraphicsAdapter.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class PCIVGACompatibleAdapter : public VGACompatibleAdapter
    , public PCI::Device {
public:
    static NonnullRefPtr<PCIVGACompatibleAdapter> initialize_with_preset_resolution(PCI::DeviceIdentifier const&, PhysicalAddress, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch);
    static NonnullRefPtr<PCIVGACompatibleAdapter> initialize(PCI::DeviceIdentifier const&);

    virtual bool framebuffer_devices_initialized() const override { return !m_framebuffer_device.is_null(); }

protected:
    explicit PCIVGACompatibleAdapter(PCI::Address);

private:
    PCIVGACompatibleAdapter(PCI::Address, PhysicalAddress, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch);

    // ^GenericGraphicsAdapter
    virtual void initialize_framebuffer_devices() override;

    virtual void enable_consoles() override;
    virtual void disable_consoles() override;

protected:
    PhysicalAddress m_framebuffer_address;
    size_t m_framebuffer_width { 0 };
    size_t m_framebuffer_height { 0 };
    size_t m_framebuffer_pitch { 0 };

    RefPtr<FramebufferDevice> m_framebuffer_device;
};
}
