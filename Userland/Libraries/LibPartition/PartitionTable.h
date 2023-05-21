/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <LibPartition/DiskPartitionMetadata.h>
#include <LibPartition/PartitionableDevice.h>

namespace Partition {

class PartitionTable {
public:
    Optional<DiskPartitionMetadata> partition(unsigned index) const;
    size_t partitions_count() const { return m_partitions.size(); }
    virtual ~PartitionTable() = default;
    virtual bool is_valid() const = 0;

    Vector<DiskPartitionMetadata> partitions() const { return m_partitions; }
    size_t block_size() const { return m_device.block_size(); }

protected:
    explicit PartitionTable(PartitionableDevice&&);
    PartitionableDevice m_device;

    Vector<DiskPartitionMetadata> m_partitions;
};

}
