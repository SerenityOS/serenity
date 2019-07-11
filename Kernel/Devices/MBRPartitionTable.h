#pragma once

#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <Kernel/Devices/DiskDevice.h>
#include <Kernel/Devices/DiskPartition.h>

#define MBR_SIGNATURE 0xaa55

struct MBRPartitionEntry {
    u8 status;
    u8 chs1[3];
    u8 type;
    u8 chs2[3];
    u32 offset;
    u32 length;
} __attribute__((packed));

struct MBRPartitionHeader {
    u8 code1[218];
    u16 ts_zero;
    u8 ts_drive, ts_seconds, ts_minutes, ts_hours;
    u8 code2[216];
    u32 disk_signature;
    u16 disk_signature_zero;
    MBRPartitionEntry entry[4];
    u16 mbr_signature;
} __attribute__((packed));

class MBRPartitionTable {
    AK_MAKE_ETERNAL

public:
    MBRPartitionTable(NonnullRefPtr<DiskDevice>);
    ~MBRPartitionTable();

    bool initialize();
    RefPtr<DiskPartition> partition(unsigned index);

private:
    NonnullRefPtr<DiskDevice> m_device;

    ByteBuffer read_header() const;
    const MBRPartitionHeader& header() const;

    u8 m_cached_header[512];
};
