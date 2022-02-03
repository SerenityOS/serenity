/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Graphics/Console/Console.h>
#include <Kernel/Graphics/FramebufferDevice.h>
#include <Kernel/Graphics/GenericGraphicsAdapter.h>
#include <Kernel/Graphics/VGA/GenericDisplayConnector.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class PCIVGAGenericAdapter : public GenericGraphicsAdapter
    , public PCI::Device {
public:
    static NonnullRefPtr<PCIVGAGenericAdapter> must_create_with_preset_resolution(PCI::DeviceIdentifier const&, PhysicalAddress, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch);
    static NonnullRefPtr<PCIVGAGenericAdapter> must_create(PCI::DeviceIdentifier const&);

    virtual bool framebuffer_devices_initialized() const override { return !m_framebuffer_device.is_null(); }

    virtual bool modesetting_capable() const override { return false; }
    virtual bool double_framebuffering_capable() const override { return false; }

    virtual bool vga_compatible() const override final { return true; }

    virtual ErrorOr<void> set_resolution(size_t, size_t, size_t) override { return Error::from_errno(ENOTSUP); }
    virtual ErrorOr<void> set_y_offset(size_t, size_t) override { return Error::from_errno(ENOTSUP); }

    ErrorOr<ByteBuffer> get_edid(size_t) const override { return Error::from_errno(ENOTSUP); }

protected:
    explicit PCIVGAGenericAdapter(PCI::Address);

private:
    ErrorOr<void> initialize_adapter_with_preset_resolution(PhysicalAddress, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch);
    ErrorOr<void> initialize_adapter();

    // ^GenericGraphicsAdapter
    virtual void enable_consoles() override;
    virtual void disable_consoles() override;
    virtual void initialize_framebuffer_devices() override;

    // Note: This is only used in PCIVGAGenericAdapter code because we need
    // to remember how to access the framebuffer
    Optional<PhysicalAddress> m_framebuffer_address;

protected:
    RefPtr<FramebufferDevice> m_framebuffer_device;
    OwnPtr<VGAGenericDisplayConnector> m_display_connector;
};
}
