/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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
#include <Kernel/Debug.h>
#include <Kernel/Storage/Partition/MBRPartitionTable.h>

namespace Kernel {

#define MBR_SIGNATURE 0xaa55
#define MBR_PROTECTIVE 0xEE
#define EBR_CHS_CONTAINER 0x05
#define EBR_LBA_CONTAINER 0x0F

Result<NonnullOwnPtr<MBRPartitionTable>, PartitionTable::Error> MBRPartitionTable::try_to_initialize(const StorageDevice& device)
{
    auto table = make<MBRPartitionTable>(device);
    if (table->contains_ebr())
        return { PartitionTable::Error::ConatinsEBR };
    if (table->is_protective_mbr())
        return { PartitionTable::Error::MBRProtective };
    if (!table->is_valid())
        return { PartitionTable::Error::Invalid };
    return table;
}

OwnPtr<MBRPartitionTable> MBRPartitionTable::try_to_initialize(const StorageDevice& device, u32 start_lba)
{
    auto table = make<MBRPartitionTable>(device, start_lba);
    if (!table->is_valid())
        return {};
    return table;
}

bool MBRPartitionTable::read_boot_record()
{
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(m_cached_header.data());
    if (!m_device->read_block(m_start_lba, buffer))
        return false;
    m_header_valid = true;
    return m_header_valid;
}

MBRPartitionTable::MBRPartitionTable(const StorageDevice& device, u32 start_lba)
    : PartitionTable(device)
    , m_start_lba(start_lba)
    , m_cached_header(ByteBuffer::create_zeroed(m_device->block_size()))
{
    if (!read_boot_record() || !initialize())
        return;

    m_header_valid = true;

    auto& header = this->header();
    for (size_t index = 0; index < 4; index++) {
        auto& entry = header.entry[index];
        if (entry.offset == 0x00) {
            continue;
        }
        auto partition_type = ByteBuffer::create_zeroed(sizeof(u8));
        partition_type.data()[0] = entry.type;
        m_partitions.append(DiskPartitionMetadata({ entry.offset, (entry.offset + entry.length), partition_type }));
    }
    m_valid = true;
}

MBRPartitionTable::MBRPartitionTable(const StorageDevice& device)
    : PartitionTable(device)
    , m_start_lba(0)
    , m_cached_header(ByteBuffer::create_zeroed(m_device->block_size()))
{
    if (!read_boot_record() || contains_ebr() || is_protective_mbr() || !initialize())
        return;

    auto& header = this->header();
    for (size_t index = 0; index < 4; index++) {
        auto& entry = header.entry[index];
        if (entry.offset == 0x00) {
            continue;
        }
        auto partition_type = ByteBuffer::create_zeroed(sizeof(u8));
        partition_type.data()[0] = entry.type;
        m_partitions.append(DiskPartitionMetadata({ entry.offset, (entry.offset + entry.length), partition_type }));
    }
    m_valid = true;
}

MBRPartitionTable::~MBRPartitionTable()
{
}

const MBRPartitionTable::Header& MBRPartitionTable::header() const
{
    return *(const MBRPartitionTable::Header*)m_cached_header.data();
}

bool MBRPartitionTable::initialize()
{
    auto& header = this->header();
    dbgln_if(MBR_DEBUG, "Master Boot Record: mbr_signature={:#08x}", header.mbr_signature);
    if (header.mbr_signature != MBR_SIGNATURE) {
        dbgln("Master Boot Record: invalid signature");
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

}
