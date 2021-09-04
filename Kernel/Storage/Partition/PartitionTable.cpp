/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Storage/Partition/PartitionTable.h>

namespace Kernel {
PartitionTable::PartitionTable(StorageDevice const& device)
    : m_device(device)
{
}

Optional<DiskPartitionMetadata> PartitionTable::partition(unsigned index)
{
    if (index > partitions_count())
        return {};
    return m_partitions[index];
}

}
