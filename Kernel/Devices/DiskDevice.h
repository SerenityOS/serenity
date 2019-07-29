#pragma once

#include <AK/RefCounted.h>
#include <AK/Types.h>
#include <Kernel/Devices/BlockDevice.h>

// FIXME: Support 64-bit DiskOffset
typedef u32 DiskOffset;

class DiskDevice : public BlockDevice {
public:
    virtual ~DiskDevice();

    virtual unsigned block_size() const = 0;
    virtual bool read_block(unsigned index, u8*) const = 0;
    virtual bool write_block(unsigned index, const u8*) = 0;
    virtual const char* class_name() const = 0;
    bool read(DiskOffset, unsigned length, u8*) const;
    bool write(DiskOffset, unsigned length, const u8*);

    virtual bool read_blocks(unsigned index, u16 count, u8*) = 0;
    virtual bool write_blocks(unsigned index, u16 count, const u8*) = 0;

    virtual bool is_disk_device() const override { return true; };

protected:
    DiskDevice(int major, int minor);
};
