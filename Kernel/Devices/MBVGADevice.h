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
#include <LibBareMetal/Memory/PhysicalAddress.h>

class MBVGADevice final : public BlockDevice {
    AK_MAKE_ETERNAL
public:
    static MBVGADevice& the();

    MBVGADevice(PhysicalAddress addr, int pitch, int width, int height);

    virtual int ioctl(FileDescription&, unsigned request, unsigned arg) override;
    virtual KResultOr<Region*> mmap(Process&, FileDescription&, VirtualAddress preferred_vaddr, size_t offset, size_t, int prot) override;

private:
    virtual const char* class_name() const override { return "MBVGA"; }
    virtual bool can_read(const FileDescription&) const override { return true; }
    virtual bool can_write(const FileDescription&) const override { return true; }
    virtual ssize_t read(FileDescription&, u8*, ssize_t) override { return -EINVAL; }
    virtual ssize_t write(FileDescription&, const u8*, ssize_t) override { return -EINVAL; }
    virtual bool read_blocks(unsigned, u16, u8*) override { return false; }
    virtual bool write_blocks(unsigned, u16, const u8*) override { return false; }

    size_t framebuffer_size_in_bytes() const { return m_framebuffer_pitch * m_framebuffer_height; }

    PhysicalAddress m_framebuffer_address;
    int m_framebuffer_pitch { 0 };
    int m_framebuffer_width { 0 };
    int m_framebuffer_height { 0 };
};
