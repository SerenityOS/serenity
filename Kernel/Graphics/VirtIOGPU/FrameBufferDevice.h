/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/VirtIO/VirtIO.h>
#include <Kernel/Bus/VirtIO/VirtIOQueue.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Graphics/VirtIOGPU/GPU.h>

namespace Kernel::Graphics::VirtIOGPU {

class FrameBufferDevice final : public BlockDevice {
    friend class Console;
    struct Buffer {
        size_t framebuffer_offset { 0 };
        u8* framebuffer_data { nullptr };
        Protocol::Rect dirty_rect {};
        ResourceID resource_id { 0 };
    };

public:
    FrameBufferDevice(VirtIOGPU::GPU& virtio_gpu, ScanoutID);
    virtual ~FrameBufferDevice() override;

    virtual void deactivate_writes();
    virtual void activate_writes();

    bool try_to_set_resolution(size_t width, size_t height);
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
    virtual StringView class_name() const override { return "VirtIOFrameBuffer"; }

    Protocol::DisplayInfoResponse::Display const& display_info() const;
    Protocol::DisplayInfoResponse::Display& display_info();

    KResult create_framebuffer();
    void create_buffer(Buffer&, size_t, size_t);
    void set_buffer(int);

    virtual KResult ioctl(FileDescription&, unsigned request, Userspace<void*> arg) override;
    virtual KResultOr<Memory::Region*> mmap(Process&, FileDescription&, Memory::VirtualRange const&, u64 offset, int prot, bool shared) override;
    virtual bool can_read(const FileDescription&, size_t) const override { return true; }
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override { return EINVAL; }
    virtual bool can_write(const FileDescription&, size_t) const override { return true; }
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return EINVAL; };
    virtual void start_request(AsyncBlockDeviceRequest& request) override { request.complete(AsyncDeviceRequest::Failure); }

    virtual mode_t required_mode() const override { return 0666; }
    virtual String device_name() const override { return String::formatted("fb{}", minor()); }

    static bool is_valid_buffer_index(int buffer_index)
    {
        return buffer_index == 0 || buffer_index == 1;
    }

    Buffer& buffer_from_index(int buffer_index)
    {
        return buffer_index == 0 ? m_main_buffer : m_back_buffer;
    }
    Buffer& current_buffer() const { return *m_current_buffer; }

    GPU& m_gpu;
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
