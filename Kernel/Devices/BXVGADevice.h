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

class BXVGADevice final : public BlockDevice {
    AK_MAKE_ETERNAL
public:
    static void initialize();
    static BXVGADevice& the();

    BXVGADevice();

    virtual int ioctl(FileDescription&, unsigned request, FlatPtr arg) override;
    virtual KResultOr<Region*> mmap(Process&, FileDescription&, const Range&, u64 offset, int prot, bool shared) override;

    // ^Device
    virtual mode_t required_mode() const override { return 0660; }
    virtual String device_name() const override;

private:
    virtual const char* class_name() const override { return "BXVGA"; }
    virtual bool can_read(const FileDescription&, size_t) const override { return true; }
    virtual bool can_write(const FileDescription&, size_t) const override { return true; }
    virtual void start_request(AsyncBlockDeviceRequest& request) override { request.complete(AsyncDeviceRequest::Failure); }
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override { return -EINVAL; }
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return -EINVAL; }

    void set_safe_resolution();

    void set_register(u16 index, u16 value);
    u16 get_register(u16 index);
    bool validate_setup_resolution(size_t width, size_t height);
    u32 find_framebuffer_address();
    void revert_resolution();
    bool test_resolution(size_t width, size_t height);
    size_t framebuffer_size_in_bytes() const { return m_framebuffer_pitch * m_framebuffer_height * 2; }
    bool set_resolution(size_t width, size_t height);
    void set_resolution_registers(size_t width, size_t height);
    void set_y_offset(size_t);

    PhysicalAddress m_framebuffer_address;
    size_t m_framebuffer_pitch { 0 };
    size_t m_framebuffer_width { 0 };
    size_t m_framebuffer_height { 0 };
    size_t m_y_offset { 0 };
};

}
