/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Types.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Graphics/GraphicsDevice.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class FramebufferDevice : public BlockDevice {
    AK_MAKE_ETERNAL
public:
    virtual int ioctl(FileDescription&, unsigned request, FlatPtr arg) override;
    virtual KResultOr<Region*> mmap(Process&, FileDescription&, const Range&, u64 offset, int prot, bool shared) override;

    // ^Device
    virtual mode_t required_mode() const override { return 0660; }
    virtual String device_name() const override;

    virtual size_t framebuffer_size_in_bytes() const { return m_framebuffer_pitch * m_framebuffer_height; }

    virtual ~FramebufferDevice() {};

protected:
    virtual bool set_resolution(size_t framebuffer_width, size_t framebuffer_height, size_t framebuffer_pitch);

    FramebufferDevice(PhysicalAddress, size_t, size_t, size_t);

    virtual bool can_read(const FileDescription&, size_t) const override final { return true; }
    virtual bool can_write(const FileDescription&, size_t) const override final { return true; }
    virtual void start_request(AsyncBlockDeviceRequest& request) override final { request.complete(AsyncDeviceRequest::Failure); }
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override { return -EINVAL; }
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return -EINVAL; }

protected:
    PhysicalAddress m_framebuffer_address;
    size_t m_framebuffer_pitch { 0 };
    size_t m_framebuffer_width { 0 };
    size_t m_framebuffer_height { 0 };
};

}
