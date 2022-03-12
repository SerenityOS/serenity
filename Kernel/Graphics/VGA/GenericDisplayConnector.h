/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Try.h>
#include <Kernel/Graphics/Console/GenericFramebufferConsole.h>
#include <Kernel/Graphics/Definitions.h>
#include <Kernel/Graphics/DisplayConnector.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

class VGAGenericDisplayConnector
    : public DisplayConnector {
    friend class DeviceManagement;

public:
    static NonnullRefPtr<VGAGenericDisplayConnector> must_create_with_preset_resolution(PhysicalAddress framebuffer_address, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch);
    static NonnullRefPtr<VGAGenericDisplayConnector> must_create();

    // ^DisplayConnector
    virtual bool modesetting_capable() const override { return false; }
    virtual bool double_framebuffering_capable() const override { return false; }
    virtual bool flush_support() const override { return false; }
    virtual bool partial_flush_support() const override { return false; }
    // Note: Bare metal hardware probably require a defined refresh rate for modesetting.
    // However, because this connector doesn't support such capability, this is safe
    // to just advertise this as not supporting refresh rate of the connected display.
    virtual bool refresh_rate_support() const override { return false; }

    virtual ErrorOr<ByteBuffer> get_edid() const override { return Error::from_errno(ENOTSUP); }
    virtual ErrorOr<void> set_resolution(Resolution const&) override { return Error::from_errno(ENOTSUP); }
    virtual ErrorOr<void> set_safe_resolution() override { return Error::from_errno(ENOTSUP); }
    virtual ErrorOr<Resolution> get_resolution() override;
    virtual ErrorOr<void> set_y_offset(size_t) override { return Error::from_errno(ENOTSUP); }
    // FIXME: If we operate in VGA mode, we actually can unblank the screen!
    virtual ErrorOr<void> unblank() override { return Error::from_errno(ENOTSUP); }

private:
    ErrorOr<void> create_attached_text_console();
    ErrorOr<void> create_attached_framebuffer_console();
    VGAGenericDisplayConnector(PhysicalAddress framebuffer_address, size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch);
    VGAGenericDisplayConnector();

    virtual ErrorOr<size_t> write_to_first_surface(u64 offset, UserOrKernelBuffer const&, size_t length) override;
    virtual ErrorOr<void> flush_first_surface() override;

    virtual void enable_console() override;
    virtual void disable_console() override;

protected:
    explicit VGAGenericDisplayConnector(PhysicalAddress framebuffer_address);

    Optional<PhysicalAddress> const m_framebuffer_address;
    size_t m_framebuffer_width { 0 };
    size_t m_framebuffer_height { 0 };
    size_t m_framebuffer_pitch { 0 };
    OwnPtr<Memory::Region> m_framebuffer_region;

    RefPtr<Graphics::Console> m_framebuffer_console;
};
}
