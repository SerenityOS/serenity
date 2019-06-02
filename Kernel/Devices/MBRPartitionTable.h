#pragma once

#include <AK/RetainPtr.h>
#include <AK/Vector.h>
#include <Kernel/Devices/DiskDevice.h>
#include <Kernel/Devices/DiskPartition.h>

#define MBR_SIGNATURE 0xaa55

struct MBRPartitionEntry {
    byte status;
    byte chs1[3];
    byte type;
    byte chs2[3];
    dword offset;
    dword length;
} __attribute__((packed));

struct MBRPartitionHeader {
    byte code1[218];
    word ts_zero;
    byte ts_drive, ts_seconds, ts_minutes, ts_hours;
    byte code2[216];
    dword disk_signature;
    word disk_signature_zero;
    MBRPartitionEntry entry[4];
    word mbr_signature;
} __attribute__((packed));

class MBRPartitionTable : public Retainable<MBRPartitionTable> {
public:
    static Retained<MBRPartitionTable> create(Retained<DiskDevice>&& device);
    virtual ~MBRPartitionTable();

    bool initialize();
    RetainPtr<DiskPartition> partition(unsigned index);

private:
    virtual const char* class_name() const;

    MBRPartitionTable(Retained<DiskDevice>&&);

    Retained<DiskDevice> m_device;

    ByteBuffer read_header() const;
    const MBRPartitionHeader& header() const;

    mutable ByteBuffer m_cached_header;
};
