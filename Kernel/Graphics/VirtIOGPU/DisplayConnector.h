/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BinaryBufferWriter.h>
#include <AK/DistinctNumeric.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Graphics/Console/Console.h>
#include <Kernel/Graphics/DisplayConnector.h>
#include <Kernel/Graphics/VirtIOGPU/GraphicsAdapter.h>
#include <Kernel/Graphics/VirtIOGPU/Protocol.h>
#include <Kernel/Memory/Region.h>
#include <LibEDID/EDID.h>

namespace Kernel::Graphics::VirtIOGPU {

class Console;

}

namespace Kernel {

class VirtIOGraphicsAdapter;
class VirtIODisplayConnector final : public DisplayConnector {
    friend class Graphics::VirtIOGPU::Console;
    friend class DeviceManagement;

private:
    struct Buffer {
        size_t framebuffer_offset { 0 };
        u8* framebuffer_data { nullptr };
        Graphics::VirtIOGPU::Protocol::Rect dirty_rect {};
        Graphics::VirtIOGPU::ResourceID resource_id { 0 };
    };

public:
    static NonnullRefPtr<VirtIODisplayConnector> must_create(VirtIOGraphicsAdapter& graphics_adapter, Graphics::VirtIOGPU::ScanoutID scanout_id);

    void set_edid_bytes(Badge<VirtIOGraphicsAdapter>, Array<u8, 128> const& edid_bytes);
    void set_safe_mode_setting_after_initialization(Badge<VirtIOGraphicsAdapter>);
    Graphics::VirtIOGPU::Protocol::DisplayInfoResponse::Display display_information(Badge<VirtIOGraphicsAdapter>);

private:
    void initialize_console();
    virtual bool mutable_mode_setting_capable() const override { return true; }
    virtual bool double_framebuffering_capable() const override { return true; }
    virtual bool partial_flush_support() const override { return true; }
    virtual ErrorOr<void> set_mode_setting(ModeSetting const&) override;
    virtual ErrorOr<void> set_safe_mode_setting() override;
    virtual ErrorOr<void> set_y_offset(size_t y) override;
    virtual ErrorOr<void> unblank() override;

    // Note: VirtIO hardware requires a constant refresh to keep the screen in sync to the user.
    virtual bool flush_support() const override { return true; }
    // Note: Paravirtualized hardware doesn't require a defined refresh rate for modesetting.
    virtual bool refresh_rate_support() const override { return false; }

    virtual ErrorOr<size_t> write_to_first_surface(u64 offset, UserOrKernelBuffer const&, size_t length) override;
    virtual ErrorOr<void> flush_first_surface() override;
    virtual ErrorOr<void> flush_rectangle(size_t buffer_index, FBRect const& rect) override;

    virtual void enable_console() override;
    virtual void disable_console() override;

private:
    VirtIODisplayConnector(VirtIOGraphicsAdapter& graphics_adapter, Graphics::VirtIOGPU::ScanoutID scanout_id);

    void query_display_information();
    ErrorOr<void> query_edid_from_virtio_adapter();
    void query_display_edid();

    void flush_dirty_window(Graphics::VirtIOGPU::Protocol::Rect const&, Buffer&);
    void transfer_framebuffer_data_to_host(Graphics::VirtIOGPU::Protocol::Rect const&, Buffer&);
    void flush_displayed_image(Graphics::VirtIOGPU::Protocol::Rect const&, Buffer&);

    // Basic 2D framebuffer methods
    static size_t calculate_framebuffer_size(size_t width, size_t height)
    {
        // VirtIO resources can only map on page boundaries!
        return Memory::page_round_up(sizeof(u32) * width * height).value();
    }
    u8* framebuffer_data();
    void draw_ntsc_test_pattern(Buffer&);
    void clear_to_black(Buffer&);
    ErrorOr<void> create_framebuffer();
    void create_buffer(Buffer&, size_t, size_t);
    void set_buffer(int);
    static bool is_valid_buffer_index(int buffer_index)
    {
        return buffer_index == 0 || buffer_index == 1;
    }
    Buffer& buffer_from_index(int buffer_index)
    {
        return buffer_index == 0 ? m_main_buffer : m_back_buffer;
    }
    Buffer& current_buffer() const { return *m_current_buffer; }

    // Member data
    // Context used for kernel operations (e.g. flushing resources to scanout)
    Graphics::VirtIOGPU::ContextID m_kernel_context_id;

    NonnullRefPtr<VirtIOGraphicsAdapter> m_graphics_adapter;
    RefPtr<Graphics::Console> m_console;
    Graphics::VirtIOGPU::Protocol::DisplayInfoResponse::Display m_display_info {};
    Graphics::VirtIOGPU::ScanoutID m_scanout_id;

    // 2D framebuffer Member data
    size_t m_buffer_size { 0 };
    Buffer* m_current_buffer { nullptr };
    Atomic<int, AK::memory_order_relaxed> m_last_set_buffer_index { 0 };
    Buffer m_main_buffer;
    Buffer m_back_buffer;
    OwnPtr<Memory::Region> m_framebuffer;
    RefPtr<Memory::VMObject> m_framebuffer_sink_vmobject;

    constexpr static size_t NUM_TRANSFER_REGION_PAGES = 256;
};

}
