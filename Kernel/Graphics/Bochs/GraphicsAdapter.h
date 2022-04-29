/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Graphics/Bochs/Definitions.h>
#include <Kernel/Graphics/Console/GenericFramebufferConsole.h>
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

    // FIXME: Remove all of these methods when we get rid of the FramebufferDevice class.
    virtual bool framebuffer_devices_initialized() const override { return false; }
    virtual bool modesetting_capable() const override { return true; }
    virtual bool double_framebuffering_capable() const override { return true; }

    virtual bool vga_compatible() const override;

private:
    ErrorOr<ByteBuffer> get_edid(size_t output_port_index) const override;

    ErrorOr<void> initialize_adapter(PCI::DeviceIdentifier const&);

    // ^GenericGraphicsAdapter
    // FIXME: Remove all of these methods when we get rid of the FramebufferDevice class.
    virtual bool try_to_set_resolution(size_t, size_t, size_t) override { VERIFY_NOT_REACHED(); }
    virtual bool set_y_offset(size_t, size_t) override { VERIFY_NOT_REACHED(); }
    virtual void initialize_framebuffer_devices() override { }
    virtual void enable_consoles() override { }
    virtual void disable_consoles() override { }

    explicit BochsGraphicsAdapter(PCI::DeviceIdentifier const&);

    RefPtr<BochsDisplayConnector> m_display_connector;
    bool m_is_vga_capable { false };
};
}
