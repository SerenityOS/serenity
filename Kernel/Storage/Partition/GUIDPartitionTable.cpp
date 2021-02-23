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

#include <AK/AllOf.h>
#include <AK/Array.h>
#include <Kernel/Debug.h>
#include <Kernel/Storage/Partition/GUIDPartitionTable.h>

namespace Kernel {

#define GPT_SIGNATURE2 0x54524150
#define GPT_SIGNATURE 0x20494645
#define BytesPerSector 512

struct [[gnu::packed]] GPTPartitionEntry {
    u8 partition_guid[16];
    u8 unique_guid[16];

    u64 first_lba;
    u64 last_lba;

    u64 attributes;
    char partition_name[72];
};

struct [[gnu::packed]] GUIDPartitionHeader {
    u32 sig[2];
    u32 revision;
    u32 header_size;
    u32 crc32_header;
    u32 reserved;
    u64 current_lba;
    u64 backup_lba;

    u64 first_usable_lba;
    u64 last_usable_lba;

    u64 disk_guid1[2];

    u64 partition_array_start_lba;

    u32 entries_count;
    u32 partition_entry_size;
    u32 crc32_entries_array;
};

Result<NonnullOwnPtr<GUIDPartitionTable>, PartitionTable::Error> GUIDPartitionTable::try_to_initialize(const StorageDevice& device)
{
    auto table = make<GUIDPartitionTable>(device);
    if (!table->is_valid())
        return { PartitionTable::Error::Invalid };
    return table;
}

GUIDPartitionTable::GUIDPartitionTable(const StorageDevice& device)
    : MBRPartitionTable(device)
{
    m_cached_header = ByteBuffer::create_zeroed(m_device->block_size());
    VERIFY(partitions_count() == 0);
    if (!initialize())
        m_valid = false;
}

const GUIDPartitionHeader& GUIDPartitionTable::header() const
{
    return *(const GUIDPartitionHeader*)m_cached_header.data();
}

bool GUIDPartitionTable::initialize()
{
    VERIFY(m_cached_header.data() != nullptr);

    auto first_gpt_block = (m_device->block_size() == 512) ? 1 : 0;

    auto buffer = UserOrKernelBuffer::for_kernel_buffer(m_cached_header.data());
    if (!m_device->read_block(first_gpt_block, buffer)) {
        return false;
    }

#if GPT_DEBUG
    klog() << "GUIDPartitionTable: signature - 0x" << String::format("%x", header().sig[1]) << String::format("%x", header().sig[0]);
#endif

    if (header().sig[0] != GPT_SIGNATURE && header().sig[1] != GPT_SIGNATURE2) {
        klog() << "GUIDPartitionTable: bad signature 0x" << String::format("%x", header().sig[1]) << String::format("%x", header().sig[0]);
        return false;
    }

    auto entries_buffer = ByteBuffer::create_zeroed(m_device->block_size());
    auto raw_entries_buffer = UserOrKernelBuffer::for_kernel_buffer(entries_buffer.data());
    size_t raw_byte_index = header().partition_array_start_lba * m_device->block_size();
    for (size_t entry_index = 0; entry_index < header().entries_count; entry_index++) {

        if (!m_device->read_block((raw_byte_index / m_device->block_size()), raw_entries_buffer)) {
            return false;
        }
        auto* entries = (const GPTPartitionEntry*)entries_buffer.data();
        auto& entry = entries[entry_index % (m_device->block_size() / (size_t)header().partition_entry_size)];
        Array<u8, 16> partition_type {};
        partition_type.span().overwrite(0, entry.partition_guid, partition_type.size());

        if (is_unused_entry(partition_type)) {
            raw_byte_index += header().partition_entry_size;
            continue;
        }

        Array<u8, 16> unique_guid {};
        unique_guid.span().overwrite(0, entry.unique_guid, unique_guid.size());
        String name = entry.partition_name;
        dbgln("Detected GPT partition (entry={}), offset={}, limit={}", entry_index, entry.first_lba, entry.last_lba);
        m_partitions.append({ entry.first_lba, entry.last_lba, partition_type, unique_guid, entry.attributes, "" });
        raw_byte_index += header().partition_entry_size;
    }

    return true;
}

bool GUIDPartitionTable::is_unused_entry(Array<u8, 16> partition_type) const
{
    return all_of(partition_type.begin(), partition_type.end(), [](const auto octet) { return octet == 0; });
}

}
