/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibPartition/MBRPartitionTable.h>

namespace Partition {

struct GUIDPartitionHeader;
class GUIDPartitionTable final : public MBRPartitionTable {
public:
    virtual ~GUIDPartitionTable() = default;

#ifdef KERNEL
    static ErrorOr<NonnullOwnPtr<GUIDPartitionTable>> try_to_initialize(Kernel::StorageDevice const&);
    explicit GUIDPartitionTable(Kernel::StorageDevice const&);
#else
    static ErrorOr<NonnullOwnPtr<GUIDPartitionTable>> try_to_initialize(NonnullRefPtr<Core::DeprecatedFile>);
    explicit GUIDPartitionTable(NonnullRefPtr<Core::DeprecatedFile>);
#endif

    virtual bool is_valid() const override
    {
        return m_valid;
    }

private:
    bool is_unused_entry(Array<u8, 16>) const;
    GUIDPartitionHeader const& header() const;
    bool initialize();

    bool m_valid { true };
    ByteBuffer m_cached_header;
};

}
