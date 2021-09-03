/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/NonnullOwnPtr.h>
#include <YAK/RefPtr.h>
#include <YAK/Result.h>
#include <YAK/Vector.h>
#include <Kernel/Storage/Partition/DiskPartition.h>
#include <Kernel/Storage/Partition/MBRPartitionTable.h>

namespace Kernel {

struct EBRPartitionHeader;
class EBRPartitionTable : public MBRPartitionTable {
public:
    ~EBRPartitionTable();

    static Result<NonnullOwnPtr<EBRPartitionTable>, PartitionTable::Error> try_to_initialize(const StorageDevice&);
    explicit EBRPartitionTable(const StorageDevice&);
    virtual bool is_valid() const override { return m_valid; };

private:
    void search_extended_partition(const StorageDevice&, MBRPartitionTable&, u64, size_t limit);

    bool m_valid { false };
};
}
