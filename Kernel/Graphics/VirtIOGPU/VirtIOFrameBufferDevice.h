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
    VirtIOFrameBufferDevice(RefPtr<VirtIOGPU> virtio_gpu);
    virtual ~VirtIOFrameBufferDevice() override;

    virtual void deactivate_writes();
    virtual void activate_writes();

private:
    virtual const char* class_name() const override { return "VirtIOFrameBuffer"; }

    virtual int ioctl(FileDescription&, unsigned request, FlatPtr arg) override;
    virtual KResultOr<Region*> mmap(Process&, FileDescription&, const Range&, u64 offset, int prot, bool shared) override;
    virtual bool can_read(const FileDescription&, size_t) const override { return true; }
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override { return EINVAL; }
    virtual bool can_write(const FileDescription&, size_t) const override { return true; }
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return EINVAL; };
    virtual void start_request(AsyncBlockDeviceRequest& request) override { request.complete(AsyncDeviceRequest::Failure); }

    virtual mode_t required_mode() const override { return 0666; }
    virtual String device_name() const override { return String::formatted("fb{}", minor()); }

    RefPtr<VirtIOGPU> m_gpu;
    RefPtr<VMObject> m_framebuffer_sink_vmobject;
    bool m_are_writes_active { true };
    // FIXME: This needs to be cleaned up if the WindowServer exits while we are in a tty
    WeakPtr<Region> m_userspace_mmap_region;
};

}
