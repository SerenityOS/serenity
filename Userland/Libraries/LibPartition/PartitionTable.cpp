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

#ifdef KERNEL
PartitionTable::PartitionTable(Kernel::StorageDevice const& device)
    : m_device(device)
    , m_block_size(device.block_size())
{
}
#else
PartitionTable::PartitionTable(NonnullRefPtr<Core::File> device_file)
    : m_device_file(device_file)
{
    VERIFY(ioctl(m_device_file->leak_fd(), STORAGE_DEVICE_GET_BLOCK_SIZE, &m_block_size) >= 0);
}
#endif

Optional<DiskPartitionMetadata> PartitionTable::partition(unsigned index) const
{
    if (index > partitions_count())
        return {};
    return m_partitions[index];
}

}
