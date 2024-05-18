/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibPartition/MBRPartitionTable.h>

namespace Partition {

#define MBR_SIGNATURE 0xaa55
#define MBR_PROTECTIVE 0xEE
#define EBR_CHS_CONTAINER 0x05
#define EBR_LBA_CONTAINER 0x0F

ErrorOr<NonnullOwnPtr<MBRPartitionTable>> MBRPartitionTable::try_to_initialize(PartitionableDevice device)
{
    auto table = TRY(adopt_nonnull_own_or_enomem(new (nothrow) MBRPartitionTable(move(device))));
    if (table->contains_ebr())
        return Error::from_errno(ENOTSUP);
    if (table->is_protective_mbr())
        return Error::from_errno(ENOTSUP);
    if (!table->is_valid())
        return Error::from_errno(EINVAL);
    return table;
}

OwnPtr<MBRPartitionTable> MBRPartitionTable::try_to_initialize(PartitionableDevice device, u32 start_lba)
{
    auto table = adopt_nonnull_own_or_enomem(new (nothrow) MBRPartitionTable(move(device), start_lba)).release_value_but_fixme_should_propagate_errors();
    if (!table->is_valid())
        return {};
    return table;
}

bool MBRPartitionTable::read_boot_record()
{
    if (block_size() != 512)
        return false;
    auto maybe_error = m_device.read_block(m_start_lba, m_cached_header.bytes());
    m_header_valid = !maybe_error.is_error();
    return m_header_valid;
}

MBRPartitionTable::MBRPartitionTable(PartitionableDevice device, u32 start_lba)
    : PartitionTable(move(device))
    , m_start_lba(start_lba)
    , m_cached_header(ByteBuffer::create_zeroed(block_size()).release_value_but_fixme_should_propagate_errors()) // FIXME: Do something sensible if this fails because of OOM.
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
        // We have to place these in stack variables, since try_empend will try to take a reference to them, which is UB (since they're gnu::packed and unaligned)
        u64 const block_offset = entry.offset;
        u64 const block_limit = (entry.offset + entry.length) - 1;
        u8 const partition_type = entry.type;
        MUST(m_partitions.try_empend(block_offset, block_limit, partition_type));
    }
    m_valid = true;
}

MBRPartitionTable::MBRPartitionTable(PartitionableDevice device)
    : PartitionTable(move(device))
    , m_start_lba(0)
    , m_cached_header(ByteBuffer::create_zeroed(block_size()).release_value_but_fixme_should_propagate_errors()) // FIXME: Do something sensible if this fails because of OOM.
{
    if (!read_boot_record() || contains_ebr() || is_protective_mbr() || !initialize())
        return;

    auto& header = this->header();
    for (size_t index = 0; index < 4; index++) {
        auto& entry = header.entry[index];
        if (entry.offset == 0x00) {
            continue;
        }
        // We have to place these in stack variables, since try_empend will try to take a reference to them, which is UB (since they're gnu::packed and unaligned)
        u64 const block_offset = entry.offset;
        u64 const block_limit = (entry.offset + entry.length) - 1;
        u8 const partition_type = entry.type;
        MUST(m_partitions.try_empend(block_offset, block_limit, partition_type));
    }
    m_valid = true;
}

MBRPartitionTable::~MBRPartitionTable() = default;

MBRPartitionTable::Header const& MBRPartitionTable::header() const
{
    return *(MBRPartitionTable::Header const*)m_cached_header.data();
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
