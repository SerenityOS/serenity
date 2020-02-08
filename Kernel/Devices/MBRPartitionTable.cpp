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

#include <AK/ByteBuffer.h>
#include <Kernel/Devices/MBRPartitionTable.h>

#define MBR_DEBUG

MBRPartitionTable::MBRPartitionTable(NonnullRefPtr<BlockDevice> device)
    : m_device(move(device))
{
}

MBRPartitionTable::~MBRPartitionTable()
{
}

const MBRPartitionHeader& MBRPartitionTable::header() const
{
    return *reinterpret_cast<const MBRPartitionHeader*>(m_cached_header);
}

bool MBRPartitionTable::initialize()
{
    if (!m_device->read_block(0, m_cached_header)) {
        return false;
    }

    auto& header = this->header();

#ifdef MBR_DEBUG
    kprintf("MBRPartitionTable::initialize: mbr_signature=%#x\n", header.mbr_signature);
#endif

    if (header.mbr_signature != MBR_SIGNATURE) {
        kprintf("MBRPartitionTable::initialize: bad mbr signature %#x\n", header.mbr_signature);
        return false;
    }

    return true;
}

bool MBRPartitionTable::contains_ebr() const
{
    for (int i = 0; i < 4; i++) {
        if (header().entry[i].type == EBR_CHS_CONTAINER || header().entry[i].type == EBR_LBA_CONTAINER)
            return true;
    }
    return false;
}

bool MBRPartitionTable::is_protective_mbr() const
{
    return header().entry[0].type == MBR_PROTECTIVE;
}

RefPtr<DiskPartition> MBRPartitionTable::partition(unsigned index)
{
    ASSERT(index >= 1 && index <= 4);

    auto& header = this->header();
    auto& entry = header.entry[index - 1];

    if (header.mbr_signature != MBR_SIGNATURE) {
        kprintf("MBRPartitionTable::initialize: bad mbr signature - not initalized? %#x\n", header.mbr_signature);
        return nullptr;
    }

#ifdef MBR_DEBUG
    kprintf("MBRPartitionTable::partition: status=%#x offset=%#x\n", entry.status, entry.offset);
#endif

    if (entry.offset == 0x00) {
#ifdef MBR_DEBUG
        kprintf("MBRPartitionTable::partition: missing partition requested index=%d\n", index);
#endif

        return nullptr;
    }

#ifdef MBR_DEBUG
    kprintf("MBRPartitionTable::partition: found partition index=%d type=%x\n", index, entry.type);
#endif

    return DiskPartition::create(m_device, entry.offset, (entry.offset + entry.length));
}
