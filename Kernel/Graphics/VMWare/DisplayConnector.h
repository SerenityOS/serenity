/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Try.h>
#include <Kernel/Graphics/Console/GenericFramebufferConsole.h>
#include <Kernel/Graphics/DisplayConnector.h>
#include <Kernel/Graphics/VMWare/GraphicsAdapter.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

class VMWareFramebufferConsole;
class VMWareDisplayConnector : public DisplayConnector {
    friend class VMWareGraphicsAdapter;
    friend class VMWareFramebufferConsole;
    friend class DeviceManagement;

public:
    static NonnullRefPtr<VMWareDisplayConnector> must_create(VMWareGraphicsAdapter const& parent_adapter, PhysicalAddress framebuffer_address);

private:
    VMWareDisplayConnector(VMWareGraphicsAdapter const& parent_adapter, PhysicalAddress framebuffer_address);
    ErrorOr<void> create_attached_framebuffer_console();

    virtual bool mutable_mode_setting_capable() const override { return true; }
    virtual bool double_framebuffering_capable() const override { return false; }
    virtual ErrorOr<void> set_mode_setting(ModeSetting const&) override;
    virtual ErrorOr<void> set_safe_mode_setting() override;
    virtual ErrorOr<void> set_y_offset(size_t y) override;
    virtual ErrorOr<void> unblank() override;

    virtual bool partial_flush_support() const override { return true; }
    virtual bool flush_support() const override { return true; }
    // Note: Paravirtualized hardware doesn't require a defined refresh rate for modesetting.
    virtual bool refresh_rate_support() const override { return false; }

    virtual ErrorOr<size_t> write_to_first_surface(u64 offset, UserOrKernelBuffer const&, size_t length) override;
    virtual ErrorOr<void> flush_first_surface() override;
    virtual ErrorOr<void> flush_rectangle(size_t buffer_index, FBRect const& rect) override;

    virtual void enable_console() override;
    virtual void disable_console() override;

private:
    u8* framebuffer_data() { return m_framebuffer_data; }

    const PhysicalAddress m_framebuffer_address;
    NonnullRefPtr<VMWareGraphicsAdapter> m_parent_adapter;
    RefPtr<VMWareFramebufferConsole> m_framebuffer_console;
    OwnPtr<Memory::Region> m_framebuffer_region;
    u8* m_framebuffer_data {};
};
}
