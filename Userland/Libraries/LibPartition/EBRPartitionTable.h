/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibPartition/MBRPartitionTable.h>

namespace Partition {

struct EBRPartitionHeader;
class EBRPartitionTable : public MBRPartitionTable {
public:
    ~EBRPartitionTable();

    static ErrorOr<NonnullOwnPtr<EBRPartitionTable>> try_to_initialize(PartitionableDevice);
    explicit EBRPartitionTable(PartitionableDevice);

    virtual bool is_valid() const override
    {
        return m_valid;
    }

private:
    void search_extended_partition(MBRPartitionTable&, u64, size_t limit);

    bool m_valid { false };
};

}
