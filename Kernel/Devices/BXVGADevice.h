/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    virtual KResultOr<Region*> mmap(Process&, FileDescription&, VirtualAddress preferred_vaddr, size_t offset, size_t, int prot, bool shared) override;

private:
    virtual const char* class_name() const override { return "BXVGA"; }
    virtual bool can_read(const FileDescription&, size_t) const override { return true; }
    virtual bool can_write(const FileDescription&, size_t) const override { return true; }
    virtual KResultOr<size_t> read(FileDescription&, size_t, UserOrKernelBuffer&, size_t) override { return -EINVAL; }
    virtual KResultOr<size_t> write(FileDescription&, size_t, const UserOrKernelBuffer&, size_t) override { return -EINVAL; }
    virtual bool read_blocks(unsigned, u16, UserOrKernelBuffer&) override { return false; }
    virtual bool write_blocks(unsigned, u16, const UserOrKernelBuffer&) override { return false; }

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
