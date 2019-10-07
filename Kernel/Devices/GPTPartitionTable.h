#pragma once

#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <Kernel/Devices/DiskDevice.h>
#include <Kernel/Devices/DiskPartition.h>

#define GPT_SIGNATURE2 0x54524150
#define GPT_SIGNATURE 0x20494645
#define BytesPerSector 512

struct GPTPartitionEntry {
    u32 partition_guid[4];
    u32 unique_guid[4];

    u32 first_lba[2];
    u32 last_lba[2];

    u64 attributes;
    u8 partition_name[72];
} __attribute__((packed));

struct GPTPartitionHeader {
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
} __attribute__((packed));

class GPTPartitionTable {

public:
    explicit GPTPartitionTable(DiskDevice&);
    ~GPTPartitionTable();

    bool initialize();
    RefPtr<DiskPartition> partition(unsigned index);

private:
    NonnullRefPtr<DiskDevice> m_device;

    const GPTPartitionHeader& header() const;

    u8 m_cached_header[512];
};
