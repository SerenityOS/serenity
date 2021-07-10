/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Graphics/VirtIOGPU/VirtIOGPU.h>
#include <Kernel/VirtIO/VirtIO.h>
#include <Kernel/VirtIO/VirtIOQueue.h>

namespace Kernel::Graphics {

class VirtIOFrameBufferDevice final : public BlockDevice {
    friend class VirtIOGPUConsole;
    struct Buffer {
        size_t framebuffer_offset { 0 };
        u8* framebuffer_data { nullptr };
        VirtIOGPUResourceID resource_id { 0 };
        VirtIOGPURect dirty_rect {};
    };

public:
    VirtIOFrameBufferDevice(VirtIOGPU& virtio_gpu, VirtIOGPUScanoutID);
    virtual ~VirtIOFrameBufferDevice() override;

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
        return page_round_up(sizeof(u32) * width * height);
    }

    void flush_dirty_window(VirtIOGPURect const&, Buffer&);
    void transfer_framebuffer_data_to_host(VirtIOGPURect const&, Buffer&);
    void flush_displayed_image(VirtIOGPURect const&, Buffer&);

    void draw_ntsc_test_pattern(Buffer&);

    u8* framebuffer_data();

private:
    virtual const char* class_name() const override { return "VirtIOFrameBuffer"; }

    VirtIOGPURespDisplayInfo::VirtIOGPUDisplayOne const& display_info() const;
    VirtIOGPURespDisplayInfo::VirtIOGPUDisplayOne& display_info();

    void create_framebuffer();
    void create_buffer(Buffer&, size_t, size_t);
    void set_buffer(int);

    virtual int ioctl(FileDescription&, unsigned request, FlatPtr arg) override;
    virtual KResultOr<Region*> mmap(Process&, FileDescription&, const Range&, u64 offset, int prot, bool shared) override;
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

    VirtIOGPU& m_gpu;
    const VirtIOGPUScanoutID m_scanout;
    Buffer* m_current_buffer { nullptr };
    Atomic<int, AK::memory_order_relaxed> m_last_set_buffer_index { 0 };
    Buffer m_main_buffer;
    Buffer m_back_buffer;
    OwnPtr<Region> m_framebuffer;
    RefPtr<VMObject> m_framebuffer_sink_vmobject;
    size_t m_buffer_size { 0 };
    bool m_are_writes_active { true };
    // FIXME: This needs to be cleaned up if the WindowServer exits while we are in a tty
    WeakPtr<Region> m_userspace_mmap_region;
};

}
