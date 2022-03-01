/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPartition/PartitionTable.h>

namespace Partition {

PartitionTable::PartitionTable(Kernel::StorageDevice const& device)
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
