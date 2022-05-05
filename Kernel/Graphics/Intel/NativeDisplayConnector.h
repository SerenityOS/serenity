/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Try.h>
#include <Kernel/Graphics/Console/GenericFramebufferConsole.h>
#include <Kernel/Graphics/Definitions.h>
#include <Kernel/Graphics/DisplayConnector.h>
#include <Kernel/Graphics/Intel/GMBusConnector.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

class IntelDisplayConnectorGroup;
class IntelNativeDisplayConnector final
    : public DisplayConnector {
    friend class IntelDisplayConnectorGroup;
    friend class DeviceManagement;

public:
    static ErrorOr<NonnullLockRefPtr<IntelNativeDisplayConnector>> try_create(IntelDisplayConnectorGroup const&, PhysicalAddress framebuffer_address, size_t framebuffer_resource_size);

    void set_edid_bytes(Badge<IntelDisplayConnectorGroup>, Array<u8, 128> const& edid_bytes);
    ErrorOr<void> create_attached_framebuffer_console(Badge<IntelDisplayConnectorGroup>);

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

    explicit IntelNativeDisplayConnector(IntelDisplayConnectorGroup const&, PhysicalAddress framebuffer_address, size_t framebuffer_resource_size);

    NonnullLockRefPtr<IntelDisplayConnectorGroup> m_parent_connector_group;
    LockRefPtr<Graphics::GenericFramebufferConsole> m_framebuffer_console;
};
}
