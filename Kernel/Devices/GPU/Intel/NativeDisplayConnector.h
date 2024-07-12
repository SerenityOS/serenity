/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Try.h>
#include <Kernel/Devices/GPU/Console/GenericFramebufferConsole.h>
#include <Kernel/Devices/GPU/Definitions.h>
#include <Kernel/Devices/GPU/DisplayConnector.h>
#include <Kernel/Devices/GPU/Intel/Auxiliary/GMBusConnector.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

class IntelDisplayConnectorGroup;
class IntelNativeDisplayConnector final
    : public DisplayConnector {
    friend class IntelDisplayConnectorGroup;
    friend class Device;

public:
    enum class Type {
        Invalid,
        Analog,
        DVO,
        LVDS,
        TVOut,
        HDMI,
        DisplayPort,
        EmbeddedDisplayPort,
    };

    enum class ConnectorIndex : size_t {
        PortA = 0,
        PortB = 1,
        PortC = 2,
        PortD = 3,
        PortE = 4,
        PortF = 5,
        PortH = 6,
        PortG = 7,
        PortI = 8,
    };

    static ErrorOr<NonnullRefPtr<IntelNativeDisplayConnector>> try_create_with_display_connector_group(IntelDisplayConnectorGroup const&, ConnectorIndex, Type, PhysicalAddress framebuffer_address, size_t framebuffer_resource_size);

    void set_edid_bytes(Badge<IntelDisplayConnectorGroup>, Array<u8, 128> const& edid_bytes);
    ErrorOr<void> create_attached_framebuffer_console(Badge<IntelDisplayConnectorGroup>);

    ConnectorIndex connector_index() const { return m_connector_index; }

private:
    // ^DisplayConnector
    // FIXME: Implement modesetting capabilities in runtime from userland...
    virtual bool mutable_mode_setting_capable() const override { return false; }
    // FIXME: Implement double buffering capabilities in runtime from userland...
    virtual bool double_framebuffering_capable() const override { return false; }
    virtual ErrorOr<void> set_mode_setting(ModeSetting const&) override;
    virtual ErrorOr<void> set_safe_mode_setting() override;
    virtual ErrorOr<void> set_y_offset(size_t y) override;
    virtual ErrorOr<void> unblank() override;
    virtual ErrorOr<void> flush_first_surface() override final;
    virtual void enable_console() override;
    virtual void disable_console() override;
    virtual bool partial_flush_support() const override { return false; }
    virtual bool flush_support() const override { return false; }
    // Note: Paravirtualized hardware doesn't require a defined refresh rate for modesetting.
    virtual bool refresh_rate_support() const override { return true; }

    IntelNativeDisplayConnector(IntelDisplayConnectorGroup const&, ConnectorIndex connector_index, Type, PhysicalAddress framebuffer_address, size_t framebuffer_resource_size);
    Type const m_type { Type::Analog };
    ConnectorIndex const m_connector_index { 0 };
    NonnullLockRefPtr<IntelDisplayConnectorGroup> m_parent_connector_group;
    LockRefPtr<Graphics::GenericFramebufferConsole> m_framebuffer_console;
};
}
