/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Try.h>
#include <Kernel/Devices/GPU/Console/GenericFramebufferConsole.h>
#include <Kernel/Devices/GPU/DisplayConnector.h>
#include <Kernel/Devices/GPU/VMWare/GraphicsAdapter.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

class VMWareFramebufferConsole;
class VMWareDisplayConnector : public DisplayConnector {
    friend class VMWareGraphicsAdapter;
    friend class VMWareFramebufferConsole;
    friend class Device;

public:
    static ErrorOr<NonnullRefPtr<VMWareDisplayConnector>> create(VMWareGraphicsAdapter const& parent_adapter, PhysicalAddress framebuffer_address, size_t framebuffer_resource_size);

private:
    VMWareDisplayConnector(VMWareGraphicsAdapter const& parent_adapter, PhysicalAddress framebuffer_address, size_t framebuffer_resource_size);
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

    virtual ErrorOr<void> flush_first_surface() override;
    virtual ErrorOr<void> flush_rectangle(size_t buffer_index, FBRect const& rect) override;

    virtual void enable_console() override;
    virtual void disable_console() override;

private:
    NonnullLockRefPtr<VMWareGraphicsAdapter> m_parent_adapter;
    LockRefPtr<VMWareFramebufferConsole> m_framebuffer_console;
};
}
