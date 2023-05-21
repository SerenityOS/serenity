/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPartition/PartitionTable.h>

#ifndef KERNEL
#    include <sys/ioctl.h>
#endif

namespace Partition {

PartitionTable::PartitionTable(PartitionableDevice&& device)
    : m_device(move(device))
{
}

Optional<DiskPartitionMetadata> PartitionTable::partition(unsigned index) const
{
    if (index > partitions_count())
        return {};
    return m_partitions[index];
}

}
