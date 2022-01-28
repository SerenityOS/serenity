/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Graphics/FramebufferDevice.h>
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
    static NonnullRefPtr<BochsGraphicsAdapter> initialize(PCI::DeviceIdentifier const&);
    virtual ~BochsGraphicsAdapter() = default;
    virtual bool framebuffer_devices_initialized() const override { return !m_framebuffer_device.is_null(); }

    virtual bool modesetting_capable() const override { return true; }
    virtual bool double_framebuffering_capable() const override { return true; }

    virtual bool vga_compatible() const override;

private:
    ErrorOr<ByteBuffer> get_edid(size_t output_port_index) const override;

    ErrorOr<void> initialize_adapter(PCI::DeviceIdentifier const&);

    // ^GenericGraphicsAdapter
    virtual ErrorOr<void> set_resolution(size_t output_port_index, size_t width, size_t height) override;
    virtual ErrorOr<void> set_y_offset(size_t output_port_index, size_t y) override;

    virtual void initialize_framebuffer_devices() override;

    virtual void enable_consoles() override;
    virtual void disable_consoles() override;

    explicit BochsGraphicsAdapter(PCI::DeviceIdentifier const&);

    OwnPtr<BochsDisplayConnector> m_display_connector;
    RefPtr<FramebufferDevice> m_framebuffer_device;

    Spinlock m_console_mode_switch_lock;
    bool m_console_enabled { false };
    bool m_is_vga_capable { false };
};
}
