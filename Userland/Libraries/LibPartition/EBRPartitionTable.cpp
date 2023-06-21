/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPartition/EBRPartitionTable.h>

namespace Partition {

ErrorOr<NonnullOwnPtr<EBRPartitionTable>> EBRPartitionTable::try_to_initialize(PartitionableDevice device)
{
    auto table = TRY(adopt_nonnull_own_or_enomem(new (nothrow) EBRPartitionTable(move(device))));
    if (table->is_protective_mbr())
        return Error::from_errno(ENOTSUP);
    if (!table->is_valid())
        return Error::from_errno(EINVAL);
    return table;
}

void EBRPartitionTable::search_extended_partition(MBRPartitionTable& checked_ebr, u64 current_block_offset, size_t limit)
{
    if (limit == 0)
        return;
    // EBRs should not carry more than 2 partitions (because they need to form a linked list)
    VERIFY(checked_ebr.partitions_count() <= 2);
    // FIXME: We should not crash the Kernel or any apps when the EBR is malformed.
    auto checked_logical_partition = checked_ebr.partition(0);

    // If we are pointed to an invalid logical partition, something is seriously wrong.
    VERIFY(checked_logical_partition.has_value());
    m_partitions.append(checked_logical_partition.value().offset(current_block_offset));
    if (!checked_ebr.contains_ebr())
        return;
    current_block_offset += checked_ebr.partition(1).value().start_block();
    auto next_ebr = MBRPartitionTable::try_to_initialize(m_device.clone_unowned(), current_block_offset);
    if (!next_ebr)
        return;
    // FIXME: Should not rely on TCO here, since this might be called from inside the Kernel, where stack space isn't exactly free.
    search_extended_partition(*next_ebr, current_block_offset, (limit - 1));
}

EBRPartitionTable::EBRPartitionTable(PartitionableDevice device)
    : MBRPartitionTable(move(device))
{
    if (!is_header_valid())
        return;
    m_valid = true;

    VERIFY(partitions_count() == 0);

    auto& header = this->header();
    for (size_t index = 0; index < 4; index++) {
        auto& entry = header.entry[index];
        // Start enumerating all logical partitions
        if (entry.type == 0xf) {
            auto checked_ebr = MBRPartitionTable::try_to_initialize(m_device.clone_unowned(), entry.offset);
            if (!checked_ebr)
                continue;
            // It's quite unlikely to see that amount of partitions, so stop at 128 partitions.
            search_extended_partition(*checked_ebr, entry.offset, 128);
            continue;
        }

        if (entry.offset == 0x00) {
            continue;
        }
        MUST(m_partitions.try_empend(entry.offset, (entry.offset + entry.length) - 1, entry.type));
    }
}

EBRPartitionTable::~EBRPartitionTable() = default;

}
