/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    , m_cached_header(ByteBuffer::create_zeroed(m_device->block_size()).release_value()) // FIXME: Do something sensible if this fails because of OOM.
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
        m_partitions.empend(entry.offset, (entry.offset + entry.length), entry.type);
    }
    m_valid = true;
}

MBRPartitionTable::MBRPartitionTable(const StorageDevice& device)
    : PartitionTable(device)
    , m_start_lba(0)
    , m_cached_header(ByteBuffer::create_zeroed(m_device->block_size()).release_value()) // FIXME: Do something sensible if this fails because of OOM.
{
    if (!read_boot_record() || contains_ebr() || is_protective_mbr() || !initialize())
        return;

    auto& header = this->header();
    for (size_t index = 0; index < 4; index++) {
        auto& entry = header.entry[index];
        if (entry.offset == 0x00) {
            continue;
        }
        m_partitions.empend(entry.offset, (entry.offset + entry.length), entry.type);
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
