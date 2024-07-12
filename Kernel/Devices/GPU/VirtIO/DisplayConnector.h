/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BinaryBufferWriter.h>
#include <AK/DistinctNumeric.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Devices/GPU/Console/Console.h>
#include <Kernel/Devices/GPU/DisplayConnector.h>
#include <Kernel/Devices/GPU/VirtIO/GraphicsAdapter.h>
#include <Kernel/Devices/GPU/VirtIO/Protocol.h>
#include <Kernel/Memory/Region.h>
#include <LibEDID/EDID.h>

namespace Kernel::Graphics::VirtIOGPU {

class Console;

}

namespace Kernel {

class VirtIOGraphicsAdapter;
class VirtIODisplayConnector final : public DisplayConnector {
    friend class Graphics::VirtIOGPU::Console;
    friend class Device;

public:
    static ErrorOr<NonnullRefPtr<VirtIODisplayConnector>> create(VirtIOGraphicsAdapter& graphics_adapter, Graphics::VirtIOGPU::ScanoutID scanout_id);

    void set_edid_bytes(Badge<VirtIOGraphicsAdapter>, Array<u8, 128> const& edid_bytes);
    void set_safe_mode_setting_after_initialization(Badge<VirtIOGraphicsAdapter>);
    Graphics::VirtIOGPU::ScanoutID scanout_id() const { return m_scanout_id; }
    Graphics::VirtIOGPU::Protocol::DisplayInfoResponse::Display display_information(Badge<VirtIOGraphicsAdapter>) const;

    void initialize_console(Badge<VirtIOGraphicsAdapter>);

private:
    virtual bool mutable_mode_setting_capable() const override { return true; }
    virtual bool double_framebuffering_capable() const override { return false; }
    virtual bool partial_flush_support() const override { return true; }
    virtual ErrorOr<void> set_mode_setting(ModeSetting const&) override;
    virtual ErrorOr<void> set_safe_mode_setting() override;
    virtual ErrorOr<void> set_y_offset(size_t y) override;
    virtual ErrorOr<void> unblank() override;

    // Note: VirtIO hardware requires a constant refresh to keep the screen in sync to the user.
    virtual bool flush_support() const override { return true; }
    // Note: Paravirtualized hardware doesn't require a defined refresh rate for modesetting.
    virtual bool refresh_rate_support() const override { return false; }

    virtual ErrorOr<void> flush_first_surface() override;
    virtual ErrorOr<void> flush_rectangle(size_t buffer_index, FBRect const& rect) override;

    virtual void enable_console() override;
    virtual void disable_console() override;

    static bool is_valid_buffer_index(size_t buffer_index)
    {
        return buffer_index == 0 || buffer_index == 1;
    }

private:
    VirtIODisplayConnector(VirtIOGraphicsAdapter& graphics_adapter, Graphics::VirtIOGPU::ScanoutID scanout_id);

    ErrorOr<void> flush_displayed_image(Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect, bool main_buffer);
    void set_dirty_displayed_rect(Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect, bool main_buffer);

    void clear_to_black();

    // Member data
    // Context used for kernel operations (e.g. flushing resources to scanout)
    Graphics::VirtIOGPU::ContextID m_kernel_context_id;

    NonnullLockRefPtr<VirtIOGraphicsAdapter> m_graphics_adapter;
    LockRefPtr<Graphics::VirtIOGPU::Console> m_console;
    Graphics::VirtIOGPU::Protocol::DisplayInfoResponse::Display m_display_info {};
    Graphics::VirtIOGPU::ScanoutID m_scanout_id;

    constexpr static size_t NUM_TRANSFER_REGION_PAGES = 256;
};

}
