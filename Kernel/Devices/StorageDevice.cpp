/*
* Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
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
#include <Kernel/Devices/StorageDevice.h>

#include <AK/ByteBuffer.h>
#include <Kernel/FileSystem/FileDescription.h>

namespace Kernel {

StorageDevice::StorageDevice(unsigned major, unsigned minor, size_t block_size)
    : BlockDevice(major, minor, block_size)
{
}

ssize_t StorageDevice::read(FileDescription& fd, u8* outbuf, ssize_t len)
{
    unsigned index = fd.offset() / block_size();
    u16 whole_blocks = len / block_size();
    ssize_t remaining = len % block_size();

    unsigned blocks_per_page = PAGE_SIZE / block_size();

    // PATAChannel will chuck a wobbly if we try to read more than PAGE_SIZE
    // at a time, because it uses a single page for its DMA buffer.
    if (whole_blocks >= blocks_per_page) {
        whole_blocks = blocks_per_page;
        remaining = 0;
    }

#ifdef PATA_DEVICE_DEBUG
    kprintf("PATADiskDevice::read() index=%d whole_blocks=%d remaining=%d\n", index, whole_blocks, remaining);
#endif

    if (whole_blocks > 0) {
        if (!read_blocks(index, whole_blocks, outbuf))
            return -1;
    }

    off_t pos = whole_blocks * block_size();

    if (remaining > 0) {
        auto buf = ByteBuffer::create_uninitialized(block_size());
        if (!read_blocks(index + whole_blocks, 1, buf.data()))
            return pos;
        memcpy(&outbuf[pos], buf.data(), remaining);
    }

    return pos + remaining;
}

bool StorageDevice::can_read(const FileDescription& fd) const
{
    return static_cast<unsigned>(fd.offset()) < capacity();
}

ssize_t StorageDevice::write(FileDescription& fd, const u8* inbuf, ssize_t len)
{
    unsigned index = fd.offset() / block_size();
    u16 whole_blocks = len / block_size();
    ssize_t remaining = len % block_size();

    unsigned blocks_per_page = PAGE_SIZE / block_size();

    // PATAChannel will chuck a wobbly if we try to write more than PAGE_SIZE
    // at a time, because it uses a single page for its DMA buffer.
    if (whole_blocks >= blocks_per_page) {
        whole_blocks = blocks_per_page;
        remaining = 0;
    }

#ifdef PATA_DEVICE_DEBUG
    kprintf("PATADiskDevice::write() index=%d whole_blocks=%d remaining=%d\n", index, whole_blocks, remaining);
#endif

    if (whole_blocks > 0) {
        if (!write_blocks(index, whole_blocks, inbuf))
            return -1;
    }

    off_t pos = whole_blocks * block_size();

    // since we can only write in block_size() increments, if we want to do a
    // partial write, we have to read the block's content first, modify it,
    // then write the whole block back to the disk.
    if (remaining > 0) {
        auto buf = ByteBuffer::create_zeroed(block_size());
        if (!read_blocks(index + whole_blocks, 1, buf.data()))
            return pos;
        memcpy(buf.data(), &inbuf[pos], remaining);
        if (!write_blocks(index + whole_blocks, 1, buf.data()))
            return pos;
    }

    return pos + remaining;
}

bool StorageDevice::can_write(const FileDescription& fd) const
{
    return static_cast<unsigned>(fd.offset()) < capacity();
}

void StorageDevice::set_drive_geometry(u16 cyls, u16 heads, u16 spt)
{
    m_cylinders = cyls;
    m_heads = heads;
    m_sectors_per_track = spt;
}

u32 StorageDevice::capacity() const
{
    return m_cylinders * m_heads * m_sectors_per_track * block_size();
}

}
