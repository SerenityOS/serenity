/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Graphics/Definitions.h>
#include <Kernel/Graphics/Intel/NativeDisplayConnector.h>
#include <Kernel/PhysicalAddress.h>
#include <LibEDID/EDID.h>

namespace Kernel {

class IntelNativeGraphicsAdapter final
    : public GenericGraphicsAdapter
    , public PCI::Device {

public:
    static RefPtr<IntelNativeGraphicsAdapter> initialize(PCI::DeviceIdentifier const&);

    virtual ~IntelNativeGraphicsAdapter() = default;

private:
    ErrorOr<void> initialize_adapter();

    explicit IntelNativeGraphicsAdapter(PCI::Address);

    // ^GenericGraphicsAdapter
    // FIXME: Remove all of these methods when we get rid of the FramebufferDevice class.
    virtual bool framebuffer_devices_initialized() const override { return false; }
    virtual bool modesetting_capable() const override { return true; }
    virtual bool vga_compatible() const override { return true; }
    virtual bool double_framebuffering_capable() const override { return true; }
    virtual bool try_to_set_resolution(size_t, size_t, size_t) override { VERIFY_NOT_REACHED(); }
    virtual bool set_y_offset(size_t, size_t) override { VERIFY_NOT_REACHED(); }
    virtual void initialize_framebuffer_devices() override { }
    virtual void enable_consoles() override { }
    virtual void disable_consoles() override { }
    virtual ErrorOr<ByteBuffer> get_edid(size_t) const override { VERIFY_NOT_REACHED(); }

    RefPtr<IntelNativeDisplayConnector> m_display_connector;
};
}
