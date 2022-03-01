/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Storage/StorageDevice.h>
#include <LibPartition/DiskPartitionMetadata.h>

namespace Partition {

class PartitionTable {
public:
    Optional<DiskPartitionMetadata> partition(unsigned index);
    size_t partitions_count() const { return m_partitions.size(); }
    virtual ~PartitionTable() = default;
    virtual bool is_valid() const = 0;

    Vector<DiskPartitionMetadata> partitions() const { return m_partitions; }

protected:
    explicit PartitionTable(Kernel::StorageDevice const&);

    NonnullRefPtr<Kernel::StorageDevice> m_device;
    Vector<DiskPartitionMetadata> m_partitions;
};

}
