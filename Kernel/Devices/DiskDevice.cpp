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

#include <Kernel/Devices/DiskDevice.h>

DiskDevice::DiskDevice(int major, int minor, size_t block_size)
    : BlockDevice(major, minor, block_size)
{
}

DiskDevice::~DiskDevice()
{
}

bool DiskDevice::read(DiskOffset offset, unsigned length, u8* out) const
{
    ASSERT((offset % block_size()) == 0);
    ASSERT((length % block_size()) == 0);
    u32 first_block = offset / block_size();
    u32 end_block = (offset + length) / block_size();
    return const_cast<DiskDevice*>(this)->read_blocks(first_block, end_block - first_block, out);
}

bool DiskDevice::write(DiskOffset offset, unsigned length, const u8* in)
{
    ASSERT((offset % block_size()) == 0);
    ASSERT((length % block_size()) == 0);
    u32 first_block = offset / block_size();
    u32 end_block = (offset + length) / block_size();
    ASSERT(first_block <= 0xffffffff);
    ASSERT(end_block <= 0xffffffff);
    return write_blocks(first_block, end_block - first_block, in);
}
