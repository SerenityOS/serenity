/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/VirtIO/Device.h>
#include <Kernel/Bus/VirtIO/Queue.h>
#include <Kernel/Graphics/FramebufferDevice.h>
#include <Kernel/Graphics/GraphicsDevice.h>
#include <Kernel/Graphics/VirtIOGPU/Protocol.h>

namespace Kernel::Graphics::VirtIOGPU {

class GraphicsAdapter;
class FramebufferDevice final : public Kernel::FramebufferDevice {
    friend class Console;
    struct Buffer {
        size_t framebuffer_offset { 0 };
        u8* framebuffer_data { nullptr };
        Protocol::Rect dirty_rect {};
        ResourceID resource_id { 0 };
    };

public:
    FramebufferDevice(GraphicsAdapter const&, ScanoutID);
    virtual ~FramebufferDevice() override;

    virtual void deactivate_writes();
    virtual void activate_writes();

    KResult try_to_set_resolution(size_t width, size_t height);
    void clear_to_black(Buffer&);

    size_t width() const { return display_info().rect.width; }
    size_t height() const { return display_info().rect.height; }
    size_t pitch() const { return display_info().rect.width * 4; }

    static size_t calculate_framebuffer_size(size_t width, size_t height)
    {
        // VirtIO resources can only map on page boundaries!
        return Memory::page_round_up(sizeof(u32) * width * height);
    }

    void flush_dirty_window(Protocol::Rect const&, Buffer&);
    void transfer_framebuffer_data_to_host(Protocol::Rect const&, Buffer&);
    void flush_displayed_image(Protocol::Rect const&, Buffer&);

    void draw_ntsc_test_pattern(Buffer&);

    u8* framebuffer_data();

private:
    virtual StringView class_name() const override { return "VirtIOFrameBuffer"sv; }

    Protocol::DisplayInfoResponse::Display const& display_info() const;
    Protocol::DisplayInfoResponse::Display& display_info();

    KResult create_framebuffer();
    void create_buffer(Buffer&, size_t, size_t);
    void set_buffer(int);

    virtual KResult ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;
    virtual KResultOr<Memory::Region*> mmap(Process&, OpenFileDescription&, Memory::VirtualRange const&, u64 offset, int prot, bool shared) override;

    static bool is_valid_buffer_index(int buffer_index)
    {
        return buffer_index == 0 || buffer_index == 1;
    }

    Buffer& buffer_from_index(int buffer_index)
    {
        return buffer_index == 0 ? m_main_buffer : m_back_buffer;
    }
    Buffer& current_buffer() const { return *m_current_buffer; }

    GraphicsAdapter const& adapter() const;
    GraphicsAdapter& adapter();

    const ScanoutID m_scanout;
    Buffer* m_current_buffer { nullptr };
    Atomic<int, AK::memory_order_relaxed> m_last_set_buffer_index { 0 };
    Buffer m_main_buffer;
    Buffer m_back_buffer;
    OwnPtr<Memory::Region> m_framebuffer;
    RefPtr<Memory::VMObject> m_framebuffer_sink_vmobject;
    size_t m_buffer_size { 0 };
    bool m_are_writes_active { true };
    // FIXME: This needs to be cleaned up if the WindowServer exits while we are in a tty
    WeakPtr<Memory::Region> m_userspace_mmap_region;
};

}
