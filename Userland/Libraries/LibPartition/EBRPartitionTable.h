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

#ifdef KERNEL
    static ErrorOr<NonnullOwnPtr<EBRPartitionTable>> try_to_initialize(Kernel::StorageDevice&);
    explicit EBRPartitionTable(Kernel::StorageDevice&);
#else
    static ErrorOr<NonnullOwnPtr<EBRPartitionTable>> try_to_initialize(NonnullRefPtr<Core::DeprecatedFile>);
    explicit EBRPartitionTable(NonnullRefPtr<Core::DeprecatedFile>);
#endif

    virtual bool is_valid() const override
    {
        return m_valid;
    }

private:
#ifdef KERNEL
    void search_extended_partition(Kernel::StorageDevice&, MBRPartitionTable&, u64, size_t limit);
#else
    void search_extended_partition(NonnullRefPtr<Core::DeprecatedFile>, MBRPartitionTable&, u64, size_t limit);
#endif

    bool m_valid { false };
};

}
