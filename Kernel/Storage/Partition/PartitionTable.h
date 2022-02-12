/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <Kernel/Storage/Partition/DiskPartition.h>
#include <Kernel/Storage/StorageDevice.h>
#include <LibPartition/DiskPartitionMetadata.h>

namespace Kernel {

class PartitionTable {
public:
    Optional<Partition::DiskPartitionMetadata> partition(unsigned index);
    size_t partitions_count() const { return m_partitions.size(); }
    virtual ~PartitionTable() = default;
    virtual bool is_valid() const = 0;

    Vector<Partition::DiskPartitionMetadata> partitions() const { return m_partitions; }

protected:
    explicit PartitionTable(StorageDevice const&);

    NonnullRefPtr<StorageDevice> m_device;
    Vector<Partition::DiskPartitionMetadata> m_partitions;
};

}
