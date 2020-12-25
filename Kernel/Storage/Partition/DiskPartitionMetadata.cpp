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

#include <Kernel/Storage/Partition/DiskPartitionMetadata.h>

namespace Kernel {
DiskPartitionMetadata::DiskPartitionMetadata(u64 start_block, u64 end_block, ByteBuffer partition_type)
    : m_start_block(start_block)
    , m_end_block(end_block)
    , m_partition_type(partition_type)
{
    ASSERT(!m_partition_type.is_empty());
}
DiskPartitionMetadata::DiskPartitionMetadata(u64 start_block, u64 end_block, ByteBuffer partition_guid, ByteBuffer unique_guid, u64 special_attributes, String name)
    : m_start_block(start_block)
    , m_end_block(end_block)
    , m_partition_type(partition_guid)
    , m_unique_guid(unique_guid)
    , m_attributes(special_attributes)
    , m_name(name)
{
    ASSERT(!m_partition_type.is_empty());
    ASSERT(!m_unique_guid.is_empty());
}
u64 DiskPartitionMetadata::start_block() const
{
    return m_start_block;
}
u64 DiskPartitionMetadata::end_block() const
{
    return m_end_block;
}
Optional<u64> DiskPartitionMetadata::special_attributes() const
{
    if (m_attributes == 0)
        return {};
    return m_attributes;
}
Optional<String> DiskPartitionMetadata::name() const
{
    if (m_name.is_null() || m_name.is_empty())
        return {};
    return m_name;
}
Optional<ByteBuffer> DiskPartitionMetadata::partition_type() const
{
    if (m_partition_type.is_null() || m_partition_type.is_empty())
        return {};
    return m_partition_type;
}
Optional<ByteBuffer> DiskPartitionMetadata::unique_guid() const
{
    if (m_unique_guid.is_null() || m_unique_guid.is_empty())
        return {};
    return m_unique_guid;
}

}
