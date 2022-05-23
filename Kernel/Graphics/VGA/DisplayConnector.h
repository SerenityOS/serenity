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
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

class GenericDisplayConnector
    : public DisplayConnector {
    friend class DeviceManagement;

public:
    static NonnullRefPtr<GenericDisplayConnector> must_create_with_preset_resolution(PhysicalAddress framebuffer_address, size_t width, size_t height, size_t pitch);

protected:
    ErrorOr<void> create_attached_framebuffer_console();

    GenericDisplayConnector(PhysicalAddress framebuffer_address, size_t width, size_t height, size_t pitch);

    virtual bool mutable_mode_setting_capable() const override final { return false; }
    virtual bool double_framebuffering_capable() const override { return false; }
    virtual ErrorOr<void> set_mode_setting(ModeSetting const&) override { return Error::from_errno(ENOTSUP); }
    virtual ErrorOr<void> set_safe_mode_setting() override { return {}; }
    virtual ErrorOr<void> set_y_offset(size_t) override { return Error::from_errno(ENOTSUP); }
    virtual ErrorOr<void> unblank() override { return Error::from_errno(ENOTSUP); }

    virtual bool partial_flush_support() const override final { return false; }
    virtual bool flush_support() const override final { return false; }
    // Note: This is possibly a paravirtualized hardware, but since we don't know, we assume there's no refresh rate...
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
