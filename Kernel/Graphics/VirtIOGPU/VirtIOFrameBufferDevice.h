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
public:
    VirtIOFrameBufferDevice(VirtIOGPU& virtio_gpu, VirtIOGPUScanoutID);
    virtual ~VirtIOFrameBufferDevice() override;

    virtual void deactivate_writes();
    virtual void activate_writes();

    bool try_to_set_resolution(size_t width, size_t height);
    void clear_to_black();

    VMObject& vm_object() { return m_framebuffer->vmobject(); }
    Region& region() { return *m_framebuffer; }

    size_t width() const { return display_info().rect.width; }
    size_t height() const { return display_info().rect.height; }
    size_t pitch() const { return display_info().rect.width * 4; }

    size_t size_in_bytes() const;
    static size_t calculate_framebuffer_size(size_t width, size_t height)
    {
        return sizeof(u32) * width * height;
    }

    void flush_dirty_window(VirtIOGPURect const&);
    void transfer_framebuffer_data_to_host(VirtIOGPURect const&);
    void flush_displayed_image(VirtIOGPURect const&);

    void draw_ntsc_test_pattern();

private:
    virtual const char* class_name() const override { return "VirtIOFrameBuffer"; }

    VirtIOGPURespDisplayInfo::VirtIOGPUDisplayOne const& display_info() const;
    VirtIOGPURespDisplayInfo::VirtIOGPUDisplayOne& display_info();

    virtual int ioctl(FileDescription&, unsigned request, FlatPtr arg) override;
    virtual KResultOr<Region*> mmap(Process&, FileDescription&, const Range&, u64 offset, int prot, bool shared) override;
    virtual bool can_read(const FileDescription&, size_t) const override { return true; }
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override { return EINVAL; }
    virtual bool can_write(const FileDescription&, size_t) const override { return true; }
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return EINVAL; };
    virtual void start_request(AsyncBlockDeviceRequest& request) override { request.complete(AsyncDeviceRequest::Failure); }

    virtual mode_t required_mode() const override { return 0666; }
    virtual String device_name() const override { return String::formatted("fb{}", minor()); }

    VirtIOGPU& m_gpu;
    const VirtIOGPUScanoutID m_scanout;
    VirtIOGPUResourceID m_resource_id { 0 };
    OwnPtr<Region> m_framebuffer;
    RefPtr<VMObject> m_framebuffer_sink_vmobject;
    bool m_are_writes_active { true };
    // FIXME: This needs to be cleaned up if the WindowServer exits while we are in a tty
    WeakPtr<Region> m_userspace_mmap_region;
};

}
