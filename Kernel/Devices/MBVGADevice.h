/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Types.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class MBVGADevice final : public BlockDevice {
    AK_MAKE_ETERNAL
public:
    static MBVGADevice& the();

    MBVGADevice(PhysicalAddress addr, size_t pitch, size_t width, size_t height);

    virtual int ioctl(FileDescription&, unsigned request, FlatPtr arg) override;
    virtual KResultOr<Region*> mmap(Process&, FileDescription&, const Range&, u64 offset, int prot, bool shared) override;

    // ^Device
    virtual mode_t required_mode() const override { return 0660; }
    virtual String device_name() const override;

private:
    virtual const char* class_name() const override { return "MBVGA"; }
    virtual bool can_read(const FileDescription&, size_t) const override { return true; }
    virtual bool can_write(const FileDescription&, size_t) const override { return true; }
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override { return -EINVAL; }
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return -EINVAL; }
    virtual void start_request(AsyncBlockDeviceRequest& request) override { request.complete(AsyncDeviceRequest::Failure); }

    size_t framebuffer_size_in_bytes() const { return m_framebuffer_pitch * m_framebuffer_height; }

    PhysicalAddress m_framebuffer_address;
    size_t m_framebuffer_pitch { 0 };
    size_t m_framebuffer_width { 0 };
    size_t m_framebuffer_height { 0 };
};

}
