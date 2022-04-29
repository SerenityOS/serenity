/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Try.h>
#include <Kernel/Graphics/Bochs/Definitions.h>
#include <Kernel/Graphics/Console/GenericFramebufferConsole.h>
#include <Kernel/Graphics/DisplayConnector.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

class BochsDisplayConnector
    : public DisplayConnector {
    friend class BochsGraphicsAdapter;
    friend class DeviceManagement;

public:
    TYPEDEF_DISTINCT_ORDERED_ID(u16, IndexID);

    static NonnullRefPtr<BochsDisplayConnector> must_create(PhysicalAddress framebuffer_address);

    virtual IndexID index_id() const;

protected:
    ErrorOr<void> create_attached_framebuffer_console();

    explicit BochsDisplayConnector(PhysicalAddress framebuffer_address);

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

    virtual ErrorOr<size_t> write_to_first_surface(u64 offset, UserOrKernelBuffer const&, size_t length) override final;
    virtual ErrorOr<void> flush_first_surface() override final;

    virtual void enable_console() override final;
    virtual void disable_console() override final;

    const PhysicalAddress m_framebuffer_address;
    RefPtr<Graphics::GenericFramebufferConsole> m_framebuffer_console;
    OwnPtr<Memory::Region> m_framebuffer_region;
    u8* m_framebuffer_data {};
};
}
