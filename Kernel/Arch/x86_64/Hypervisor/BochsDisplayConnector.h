/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Try.h>
#include <Kernel/Graphics/Bochs/Definitions.h>
#include <Kernel/Graphics/Console/GenericFramebufferConsole.h>
#include <Kernel/Graphics/DisplayConnector.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

class BochsDisplayConnector
    : public DisplayConnector {
    friend class BochsGraphicsAdapter;
    friend class DeviceManagement;
    friend class GraphicsManagement;

public:
    AK_TYPEDEF_DISTINCT_ORDERED_ID(u16, IndexID);

    static LockRefPtr<BochsDisplayConnector> try_create_for_vga_isa_connector();

    static NonnullLockRefPtr<BochsDisplayConnector> must_create(PhysicalAddress framebuffer_address, size_t framebuffer_resource_size, bool virtual_box_hardware);

private:
    IndexID index_id() const;

    ErrorOr<void> create_attached_framebuffer_console();

    BochsDisplayConnector(PhysicalAddress framebuffer_address, size_t framebuffer_resource_size);

    virtual bool mutable_mode_setting_capable() const override final { return true; }
    virtual bool double_framebuffering_capable() const override { return false; }
    virtual ErrorOr<void> set_mode_setting(ModeSetting const&) override;
    virtual ErrorOr<void> set_safe_mode_setting() override final;
    virtual ErrorOr<void> set_y_offset(size_t y) override;
    virtual ErrorOr<void> unblank() override;

    virtual bool partial_flush_support() const override final { return false; }
    virtual bool flush_support() const override final { return false; }
    // Note: Paravirtualized hardware doesn't require a defined refresh rate for modesetting.
    virtual bool refresh_rate_support() const override final { return false; }

    virtual ErrorOr<void> flush_first_surface() override final;

    virtual void enable_console() override final;
    virtual void disable_console() override final;

    LockRefPtr<Graphics::GenericFramebufferConsole> m_framebuffer_console;
};
}
