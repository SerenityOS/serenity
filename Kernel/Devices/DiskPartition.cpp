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

#include <Kernel/Devices/DiskPartition.h>

// #define OFFD_DEBUG

NonnullRefPtr<DiskPartition> DiskPartition::create(BlockDevice& device, unsigned block_offset, unsigned block_limit)
{
    return adopt(*new DiskPartition(device, block_offset, block_limit));
}

DiskPartition::DiskPartition(BlockDevice& device, unsigned block_offset, unsigned block_limit)
    : BlockDevice(100, 0, device.block_size())
    , m_device(device)
    , m_block_offset(block_offset)
    , m_block_limit(block_limit)
{
}

DiskPartition::~DiskPartition()
{
}

bool DiskPartition::read_blocks(unsigned index, u16 count, u8* out)
{
#ifdef OFFD_DEBUG
    kprintf("DiskPartition::read_blocks %u (really: %u) count=%u\n", index, m_block_offset + index, count);
#endif

    return m_device->read_blocks(m_block_offset + index, count, out);
}

bool DiskPartition::write_blocks(unsigned index, u16 count, const u8* data)
{
#ifdef OFFD_DEBUG
    kprintf("DiskPartition::write_blocks %u (really: %u) count=%u\n", index, m_block_offset + index, count);
#endif

    return m_device->write_blocks(m_block_offset + index, count, data);
}

const char* DiskPartition::class_name() const
{
    return "DiskPartition";
}
