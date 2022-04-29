/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Result.h>
#include <AK/Vector.h>
#include <Kernel/Storage/Partition/DiskPartition.h>
#include <Kernel/Storage/Partition/MBRPartitionTable.h>

namespace Kernel {

struct EBRPartitionHeader;
class EBRPartitionTable : public MBRPartitionTable {
public:
    ~EBRPartitionTable();

    static ErrorOr<NonnullOwnPtr<EBRPartitionTable>> try_to_initialize(StorageDevice const&);
    explicit EBRPartitionTable(StorageDevice const&);
    virtual bool is_valid() const override { return m_valid; };

private:
    void search_extended_partition(StorageDevice const&, MBRPartitionTable&, u64, size_t limit);

    bool m_valid { false };
};
}
