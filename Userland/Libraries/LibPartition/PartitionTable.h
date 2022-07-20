/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibPartition/DiskPartitionMetadata.h>

#ifdef KERNEL
#    include <Kernel/Storage/StorageDevice.h>
#else
#    include <LibCore/File.h>
#endif

namespace Partition {

class PartitionTable {
public:
    Optional<DiskPartitionMetadata> partition(unsigned index) const;
    size_t partitions_count() const { return m_partitions.size(); }
    virtual ~PartitionTable() = default;
    virtual bool is_valid() const = 0;

    Vector<DiskPartitionMetadata> partitions() const { return m_partitions; }
    size_t block_size() const { return m_block_size; }

protected:
#ifdef KERNEL
    explicit PartitionTable(Kernel::StorageDevice const&);
    NonnullRefPtr<Kernel::StorageDevice> m_device;
#else
    explicit PartitionTable(NonnullRefPtr<Core::File>);
    NonnullRefPtr<Core::File> m_device_file;
#endif

    Vector<DiskPartitionMetadata> m_partitions;
    size_t m_block_size;
};

}
