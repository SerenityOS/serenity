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

#include <Kernel/Devices/Device.h>

namespace Kernel {

class BlockDevice : public Device {
public:
    virtual ~BlockDevice() override;

    size_t block_size() const { return m_block_size; }
    virtual bool is_seekable() const override { return true; }

    bool read_block(unsigned index, u8*) const;
    bool write_block(unsigned index, const u8*);
    bool read_raw(u32 offset, unsigned length, u8*) const;
    bool write_raw(u32 offset, unsigned length, const u8*);

    virtual bool read_blocks(unsigned index, u16 count, u8*) = 0;
    virtual bool write_blocks(unsigned index, u16 count, const u8*) = 0;

protected:
    BlockDevice(unsigned major, unsigned minor, size_t block_size = PAGE_SIZE)
        : Device(major, minor)
        , m_block_size(block_size)
    {
    }

private:
    virtual bool is_block_device() const final { return true; }

    size_t m_block_size { 0 };
};

}
