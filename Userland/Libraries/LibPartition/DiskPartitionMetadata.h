/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/UUID.h>

namespace Partition {

class DiskPartitionMetadata {
private:
    class PartitionType {
        friend class DiskPartitionMetadata;

    public:
        explicit PartitionType(u8 partition_type);
        explicit PartitionType(Array<u8, 16> partition_type);
        UUID to_uuid() const;
        u8 to_byte_indicator() const;
        bool is_uuid() const;
        bool is_valid() const;

    private:
        Array<u8, 16> m_partition_type {};
        bool m_partition_type_is_uuid { false };
    };

public:
    DiskPartitionMetadata(u64 block_offset, u64 block_limit, u8 partition_type);
    DiskPartitionMetadata(u64 start_block, u64 end_block, Array<u8, 16> partition_type);
    DiskPartitionMetadata(u64 block_offset, u64 block_limit, Array<u8, 16> partition_type, UUID unique_guid, u64 special_attributes);
    u64 start_block() const;
    u64 end_block() const;

    DiskPartitionMetadata offset(u64 blocks_count) const;

    Optional<u64> special_attributes() const;
    PartitionType const& type() const;
    const UUID& unique_guid() const;

private:
    u64 m_start_block;
    u64 m_end_block;
    PartitionType m_type;
    UUID m_unique_guid {};
    u64 m_attributes { 0 };
};

}
