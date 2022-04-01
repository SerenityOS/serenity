/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Result.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/Storage/Partition/MBRPartitionTable.h>

namespace Kernel {

struct GUIDPartitionHeader;
class GUIDPartitionTable final : public MBRPartitionTable {
public:
    virtual ~GUIDPartitionTable() = default;
    ;

    static Result<NonnullOwnPtr<GUIDPartitionTable>, PartitionTable::Error> try_to_initialize(StorageDevice const&);
    explicit GUIDPartitionTable(StorageDevice const&);

    virtual bool is_valid() const override { return m_valid; };

private:
    bool is_unused_entry(Array<u8, 16>) const;
    GUIDPartitionHeader const& header() const;
    bool initialize();

    bool m_valid { true };
    ByteBuffer m_cached_header;
};

}
