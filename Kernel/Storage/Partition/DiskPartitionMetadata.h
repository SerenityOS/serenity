/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/UUID.h>
#include <Kernel/Devices/BlockDevice.h>

namespace Kernel {

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
    DiskPartitionMetadata(u64 block_offset, u64 block_limit, Array<u8, 16> partition_type, UUID unique_guid, u64 special_attributes, String name);
    u64 start_block() const;
    u64 end_block() const;

    DiskPartitionMetadata offset(u64 blocks_count) const;

    Optional<u64> special_attributes() const;
    Optional<String> name() const;
    const PartitionType& type() const;
    const UUID& unique_guid() const;

private:
    u64 m_start_block;
    u64 m_end_block;
    PartitionType m_type;
    UUID m_unique_guid {};
    u64 m_attributes { 0 };
    String m_name;
};

}
